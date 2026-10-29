package com.gym.system.service;

import com.gym.system.controller.helper.AppearanceHelper;
import com.gym.system.controller.helper.NavigationHelper;
import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.*;
import com.gym.system.util.AppearanceSettings;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.LocalDate;
// import java.time.YearMonth;
// import java.util.ArrayList;
import java.util.List;
// import java.util.Optional;
// import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {
    private boolean addMemberCalled;
    private boolean updateMemberCalled;
    private boolean addSubscriptionCalled;
    private boolean checkInCalled;
    private boolean addCoachCalled;
    private GymDataAccess fakeDao;
    private MemberService memberService;
    private SubscriptionService subscriptionService;
    private AttendanceService attendanceService;
    private CoachService coachService;

    @BeforeEach
    void setUp() {
        addMemberCalled = false;
        updateMemberCalled = false;
        addSubscriptionCalled = false;
        checkInCalled = false;
        addCoachCalled = false;

        fakeDao = (GymDataAccess) Proxy.newProxyInstance(
                GymDataAccess.class.getClassLoader(),
                new Class<?>[]{GymDataAccess.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("addMember".equals(name)) {
                        addMemberCalled = true;
                        return null;
                    }
                    if ("updateMember".equals(name)) {
                        updateMemberCalled = true;
                        return null;
                    }
                    if ("members".equals(name)) {
                        return List.of(new GymRepository.MemberRow(1, "Test Member", "+201234567890", "test@gym.com", "MALE", "None", "Basic", "ACTIVE"));
                    }
                    if ("addSubscription".equals(name)) {
                        addSubscriptionCalled = true;
                        return null;
                    }
                    if ("subscriptions".equals(name)) {
                        return List.of();
                    }
                    if ("checkIn".equals(name)) {
                        checkInCalled = true;
                        return new GymRepository.CheckInResult(1, "Ahmed", "Monthly", "TIME_BASED", 0, 0, "2026-01-01", "2026-02-01");
                    }
                    if ("checkInByQrCode".equals(name)) {
                        checkInCalled = true;
                        return new GymRepository.CheckInResult(1, "Ahmed", "Monthly", "TIME_BASED", 0, 0, "2026-01-01", "2026-02-01");
                    }
                    if ("addCoach".equals(name)) {
                        addCoachCalled = true;
                        return null;
                    }
                    if ("coaches".equals(name)) {
                        return List.of();
                    }
                    return null;
                }
        );

        memberService = new MemberService(fakeDao);
        subscriptionService = new SubscriptionService(fakeDao);
        attendanceService = new AttendanceService(fakeDao);
        coachService = new CoachService(fakeDao);
    }

    @Test
    void memberServiceDelegatesToRepository() throws SQLException {
        Member member = new Member(1, "Test Member", "+201234567890", "test@gym.com", "MALE", "None");
        memberService.addMember(member);
        assertTrue(addMemberCalled);

        memberService.updateMember(1, member);
        assertTrue(updateMemberCalled);

        List<GymRepository.MemberRow> list = memberService.getAllMembers();
        assertEquals(1, list.size());
        assertEquals("Test Member", list.get(0).fullName());
    }

    @Test
    void subscriptionServiceDelegatesToRepository() throws SQLException {
        Subscription sub = new Subscription(1, 1, null, "Gold Plan", Subscription.PlanType.TIME_BASED,
                1, null, null, LocalDate.now(), LocalDate.now().plusMonths(1), 500, 0, Subscription.Status.ACTIVE);
        subscriptionService.addSubscription(sub);
        assertTrue(addSubscriptionCalled);
    }

    @Test
    void attendanceServiceDelegatesToRepository() throws SQLException {
        GymRepository.CheckInResult res = attendanceService.checkIn(1);
        assertEquals("Ahmed", res.memberName());
        assertTrue(checkInCalled);

        checkInCalled = false;
        attendanceService.checkInByQrCode("MEM-0001");
        assertTrue(checkInCalled);
    }

    @Test
    void coachServiceDelegatesToRepository() throws SQLException {
        Coach coach = new Coach(1, "Captain Ali", "+201011111111", null, 6000, true);
        coachService.addCoach(coach);
        assertTrue(addCoachCalled);
    }

    @Test
    void navigationHelperRestrictsReceptionistAccess() {
        String[] sections = {"dashboard", "members", "subscriptions", "attendance", "finance", "payroll", "team"};
        var adminItems = NavigationHelper.createNavigationItems("ADMIN", key -> key, sections);
        assertEquals(7, adminItems.size());

        var receptionItems = NavigationHelper.createNavigationItems("RECEPTIONIST", key -> key, sections);
        assertEquals(4, receptionItems.size());
        assertFalse(receptionItems.stream().anyMatch(i -> "finance".equals(i.section())));
        assertFalse(receptionItems.stream().anyMatch(i -> "payroll".equals(i.section())));
        assertFalse(receptionItems.stream().anyMatch(i -> "team".equals(i.section())));
    }

    @Test
    void appearanceHelperComputesHexAndTextCorrectly() {
        assertEquals("#FFFFFF", AppearanceHelper.colorHex(Color.WHITE));
        assertEquals("#000000", AppearanceHelper.colorHex(Color.BLACK));
        assertEquals("English", AppearanceHelper.languageButtonText(true));
        assertEquals("العربية", AppearanceHelper.languageButtonText(false));

        AppearanceSettings dark = AppearanceSettings.create(true, "#FF0000");
        assertEquals("Light mode", AppearanceHelper.themeButtonText(dark, key -> "appearance.lightMode".equals(key) ? "Light mode" : "Dark mode"));
    }
}
