package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository.MemberRow;
import com.gym.system.model.Admin;
import com.gym.system.model.Member;
import com.gym.system.model.Receptionist;
import com.gym.system.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that the services enforce roles and validation without relying on the screens. */
class ServiceAuthorizationTest {

    private final List<String> calls = new ArrayList<>();
    private Member lastUpdatedMember;
    private GymDataAccess repository;
    private User signedIn;
    private AccessControl access;

    @BeforeEach
    void setUp() {
        calls.clear();
        lastUpdatedMember = null;
        signedIn = null;
        access = new AccessControl(() -> Optional.ofNullable(signedIn));
        repository = (GymDataAccess) Proxy.newProxyInstance(GymDataAccess.class.getClassLoader(),
                new Class<?>[]{GymDataAccess.class}, (proxy, method, args) -> {
                    calls.add(method.getName());
                    if ("members".equals(method.getName())) {
                        return List.of(new MemberRow(1, "Test Member", "+201234567890", "t@gym.com", "MALE",
                                "Asthma", "Basic", "ACTIVE"));
                    }
                    if ("updateMember".equals(method.getName())) {
                        lastUpdatedMember = (Member) args[1];
                    }
                    return null;
                });
    }

    private void signInAsAdmin() {
        signedIn = new Admin(1, "admin", "Administrator", null, 0);
    }

    private void signInAsReceptionist() {
        signedIn = new Receptionist(2, "reception", "Receptionist", null, 0);
    }

    @Test
    void receptionistCannotUseAdministratorOperations() {
        signInAsReceptionist();
        TeamService team = new TeamService(repository, access);
        PayrollService payroll = new PayrollService(repository, access);
        FinanceService finance = new FinanceService(repository, access);

        assertThrows(AccessDeniedException.class, team::getUsers);
        assertThrows(AccessDeniedException.class,
                () -> team.addUser("u", "pw".toCharArray(), "Name", "ADMIN", null, 0));
        assertThrows(AccessDeniedException.class, () -> team.deactivateUser(1));
        assertThrows(AccessDeniedException.class, () -> payroll.getPayroll(YearMonth.now()));
        assertThrows(AccessDeniedException.class, () -> payroll.paySalary("USER", 1, YearMonth.now()));
        assertThrows(AccessDeniedException.class, finance::getPayments);
        assertThrows(AccessDeniedException.class, () -> finance.addExpense("Rent", 100, LocalDate.now()));
        assertTrue(calls.isEmpty(), "No repository call may happen when access is denied");
    }

    @Test
    void signedOutUsersCannotUseAnyService() {
        MemberService members = new MemberService(repository, access);
        assertThrows(AccessDeniedException.class, members::getAllMembers);
        assertThrows(AccessDeniedException.class,
                () -> new AttendanceService(repository, access).checkIn(1));
        assertTrue(calls.isEmpty());
    }

    @Test
    void administratorCanUseAdministratorOperations() throws Exception {
        signInAsAdmin();
        new TeamService(repository, access).getUsers();
        new PayrollService(repository, access).getPayroll(YearMonth.now());
        new FinanceService(repository, access).getPayments();
        new FinanceService(repository, access).addExpense("Rent", 100, LocalDate.now());
        assertEquals(List.of("users", "payroll", "payments", "addExpense"), calls);
    }

    @Test
    void medicalNotesAreHiddenFromReceptionButVisibleToAdministrators() throws Exception {
        MemberService members = new MemberService(repository, access);

        signInAsReceptionist();
        assertNull(members.getAllMembers().get(0).medicalNotes());
        assertEquals("Test Member", members.getAllMembers().get(0).fullName());

        signInAsAdmin();
        assertEquals("Asthma", members.getAllMembers().get(0).medicalNotes());
    }

    @Test
    void receptionCannotOverwriteStoredMedicalNotes() throws Exception {
        MemberService members = new MemberService(repository, access);
        Member edited = new Member(1, "Test Member", "+201234567890", "t@gym.com", "MALE", "Replaced by reception");

        signInAsReceptionist();
        members.updateMember(1, edited);
        assertEquals("Asthma", lastUpdatedMember.getMedicalNotes());

        signInAsAdmin();
        members.updateMember(1, edited);
        assertEquals("Replaced by reception", lastUpdatedMember.getMedicalNotes());
    }

    @Test
    void invalidExpensesAreRejectedBeforeReachingTheRepository() {
        signInAsAdmin();
        FinanceService finance = new FinanceService(repository, access);
        LocalDate today = LocalDate.now();

        assertThrows(IllegalArgumentException.class, () -> finance.addExpense(" ", 10, today));
        assertThrows(IllegalArgumentException.class, () -> finance.addExpense(null, 10, today));
        assertThrows(IllegalArgumentException.class, () -> finance.addExpense("Rent", 0, today));
        assertThrows(IllegalArgumentException.class, () -> finance.addExpense("Rent", -5, today));
        assertThrows(IllegalArgumentException.class, () -> finance.addExpense("Rent", Double.NaN, today));
        assertThrows(IllegalArgumentException.class, () -> finance.addExpense("Rent", 10, null));
        assertTrue(calls.isEmpty());
    }

    @Test
    void subscriptionStatusCanOnlyBeToggledBetweenActiveAndFrozen() throws Exception {
        signInAsReceptionist();
        SubscriptionService subscriptions = new SubscriptionService(repository, access);

        assertThrows(IllegalArgumentException.class, () -> subscriptions.setSubscriptionStatus(1, "EXPIRED"));
        assertThrows(IllegalArgumentException.class, () -> subscriptions.setSubscriptionStatus(1, null));
        assertTrue(calls.isEmpty());

        subscriptions.setSubscriptionStatus(1, "FROZEN");
        subscriptions.setSubscriptionStatus(1, "ACTIVE");
        assertEquals(List.of("setSubscriptionStatus", "setSubscriptionStatus"), calls);
    }
}
