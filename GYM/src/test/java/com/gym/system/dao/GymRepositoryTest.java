package com.gym.system.dao;

import com.gym.system.model.User;
import com.gym.system.model.Activity;
import com.gym.system.model.Coach;
import com.gym.system.model.Member;
import com.gym.system.model.Subscription;
import com.gym.system.model.Admin;
import com.gym.system.model.Receptionist;
import com.gym.system.util.LanguageManager;
import com.gym.system.util.AppearanceSettings;
import com.gym.system.util.QrCodeService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GymRepositoryTest {
        @TempDir
        static Path temporaryDirectory;

        private static GymRepository repository;

        @Test
        void createsRoleSpecificUsersAndLoadsBothLanguages() {
                assertEquals("ADMIN", new Admin(1, "manager", "Manager", "", 0).role());
                assertEquals("RECEPTIONIST", new Receptionist(2, "desk", "Desk", "", 0).role());
                LanguageManager language = LanguageManager.instance();
                assertEquals("Members", language.text("nav.members"));
                language.setLanguage("ar");
                assertEquals("الأعضاء", language.text("nav.members"));
                assertThrows(IllegalArgumentException.class, () -> language.setLanguage("fr"));
                assertEquals("#12ABEF", AppearanceSettings.create(true, "#12abef").accentColor());
                assertThrows(IllegalArgumentException.class, () -> AppearanceSettings.create(false, "orange"));
        }

        @BeforeAll
        static void initializeDatabase() throws Exception {
                Path databaseDirectory = temporaryDirectory.resolve("data");
                DatabaseConnection.useDirectory(databaseDirectory);
                Files.createDirectories(databaseDirectory);
                try (var connection = DriverManager.getConnection(
                                "jdbc:sqlite:" + databaseDirectory.resolve("gym_system.db").toAbsolutePath());
                                var statement = connection.createStatement()) {
                        statement.execute("""
                                        CREATE TABLE users (
                                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                                            username TEXT NOT NULL UNIQUE,
                                            password_hash TEXT NOT NULL,
                                            full_name TEXT NOT NULL,
                                            role TEXT NOT NULL,
                                            phone TEXT,
                                            active INTEGER NOT NULL DEFAULT 1,
                                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        )
                                        """);
                        statement.execute("""
                                        CREATE TABLE coaches (
                                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                                            name TEXT NOT NULL,
                                            phone TEXT,
                                            activity_id INTEGER,
                                            is_personal_trainer INTEGER NOT NULL DEFAULT 0
                                        )
                                        """);
                }
                DatabaseConnection.initialize();
                repository = new GymRepository();
        }

        @Test
        void persistsMembersAndSubscriptionsAndValidatesCheckIns() throws Exception {
                assertTrue(repository.authenticate("admin", "admin123".toCharArray()).isPresent());
                assertFalse(repository.authenticate("admin", "incorrect".toCharArray()).isPresent());
                repository.addMember(new Member(0, "Test Member", "+201000000001", "member@example.com",
                                "UNSPECIFIED", ""));
                GymRepository.MemberRow member = repository.members().stream()
                                .filter(row -> row.phone().equals("+201000000001"))
                                .findFirst().orElseThrow();
                repository.updateMember(member.id(), new Member(member.id(), member.fullName(), member.phone(),
                                member.email(), "MALE", "Knee injury"));
                GymRepository.MemberRow updatedMember = repository.members().stream()
                                .filter(row -> row.id() == member.id()).findFirst().orElseThrow();
                assertEquals("MALE", updatedMember.gender());
                assertEquals("Knee injury", updatedMember.medicalNotes());

                GymRepository.ActivityRow activity = repository.activities().getFirst();
                repository.addSubscription(new Subscription(0, member.id(), activity.id(), "10 session pack",
                                Subscription.PlanType.SESSION_BASED, null, 10, 10, LocalDate.now(), null,
                                1000, 10, Subscription.Status.ACTIVE));
                GymRepository.SubscriptionRow subscription = repository.subscriptions().stream()
                                .filter(row -> row.member().equals("Test Member"))
                                .findFirst().orElseThrow();
                assertEquals(900.0, subscription.price(), 0.001);
                assertEquals("10 / 10", subscription.remaining());
                assertEquals("PENDING", subscription.paymentStatus());
                assertEquals(0, repository.dashboard().revenue(), 0.001);
                GymRepository.MemberRow overview = repository.members().stream()
                                .filter(row -> row.id() == member.id()).findFirst().orElseThrow();
                assertEquals("10 session pack", overview.plan());
                assertEquals("ACTIVE", overview.status());
                assertEquals(activity.nameEnglish(), overview.activityEnglish());
                assertEquals("10 / 10", overview.remaining());
                assertEquals("PENDING", overview.paymentStatus());
                assertEquals(1, repository.memberSubscriptions(member.id()).size());

                assertEquals("MEM-%04d".formatted(member.id()), QrCodeService.memberCode(member.id()));
                var qr = QrCodeService.createMatrix(QrCodeService.memberCode(member.id()), 256);
                int blackPixels = 0;
                for (int x = 0; x < qr.getWidth(); x++) {
                        for (int y = 0; y < qr.getHeight(); y++) {
                                if (qr.get(x, y))
                                        blackPixels++;
                        }
                }
                assertTrue(blackPixels > 1000);
                GymRepository.SubscriptionInvoice invoice = repository.subscriptionInvoice(subscription.id());
                assertEquals("Test Member", invoice.member());
                assertEquals(10, invoice.totalSessions());
                assertEquals(900.0, invoice.amount(), 0.001);
                assertEquals("PENDING", invoice.paymentStatus());
                assertTrue(invoice.paymentDate().isBlank());
                assertTrue(invoice.paymentId() > 0);
                assertEquals(activity.nameEnglish(), invoice.activityEnglish());
                GymRepository.CheckInResult checkIn = repository.checkInByQrCode(QrCodeService.memberCode(member.id()));
                assertEquals("Test Member", checkIn.memberName());
                assertEquals("10 session pack", checkIn.planName());
                assertTrue(checkIn.sessionBased());
                assertEquals(9, checkIn.remainingSessions());
                repository.confirmOfflineSubscriptionPayment(subscription.id());
                assertEquals("PAID", repository.subscriptionInvoice(subscription.id()).paymentStatus());
                assertFalse(repository.subscriptionInvoice(subscription.id()).paymentDate().isBlank());
                assertEquals(900.0, repository.dashboard().revenue(), 0.001);
                assertThrows(SQLException.class, () -> repository.confirmOfflineSubscriptionPayment(subscription.id()));
                assertThrows(IllegalArgumentException.class, () -> repository.checkInByQrCode("invalid"));
                assertEquals("9 / 10", repository.subscriptions().stream()
                                .filter(row -> row.member().equals("Test Member"))
                                .findFirst().orElseThrow().remaining());
                assertThrows(SQLException.class, () -> repository.checkIn(member.id()));
                assertEquals(1, repository.attendance().stream()
                                .filter(row -> row.member().equals("Test Member")).count());
                repository.addSubscription(new Subscription(0, member.id(), activity.id(), "Renewal pack",
                                Subscription.PlanType.SESSION_BASED, null, 5, 5, LocalDate.now(), null,
                                500, 0, Subscription.Status.ACTIVE));
                List<GymRepository.SubscriptionRow> memberSubscriptions = repository.subscriptions().stream()
                                .filter(row -> row.member().equals("Test Member")).toList();
                assertEquals(2, memberSubscriptions.size());
                assertEquals(2, repository.memberSubscriptions(member.id()).size());
                assertEquals("Renewal pack", repository.members().stream()
                                .filter(row -> row.id() == member.id()).findFirst().orElseThrow().plan());
                assertEquals(1, repository.memberVisits(member.id(), 5).size());
                assertEquals("EXPIRED", memberSubscriptions.stream()
                                .filter(row -> row.id() == subscription.id()).findFirst().orElseThrow().status());
                Member timeBasedMember = new Member(0, "Time Plan Member", "+201000000003", "",
                                "UNSPECIFIED", "");
                repository.addMember(timeBasedMember);
                GymRepository.MemberRow timeBasedRow = repository.members().stream()
                                .filter(row -> row.phone().equals(timeBasedMember.getPhone()))
                                .findFirst().orElseThrow();
                repository.addSubscription(new Subscription(0, timeBasedRow.id(), activity.id(), "Monthly gym",
                                Subscription.PlanType.TIME_BASED, 1, null, null, LocalDate.now(),
                                LocalDate.now().plusMonths(1), 500, 0, Subscription.Status.ACTIVE));
                GymRepository.CheckInResult timeCheckIn = repository
                                .checkInByQrCode(QrCodeService.memberCode(timeBasedRow.id()));
                assertFalse(timeCheckIn.sessionBased());
                assertEquals("Monthly gym", timeCheckIn.planName());
                assertEquals(LocalDate.now().plusMonths(1).toString(), timeCheckIn.endDate());
                GymRepository.SubscriptionRow timeSubscription = repository.subscriptions().stream()
                                .filter(row -> row.member().equals("Time Plan Member")).findFirst().orElseThrow();
                repository.setSubscriptionStatus(timeSubscription.id(), "FROZEN");
                assertThrows(IllegalStateException.class, () -> repository.checkIn(timeBasedRow.id()));
                repository.setSubscriptionStatus(timeSubscription.id(), "ACTIVE");
                assertThrows(SQLException.class, () -> repository.checkIn(timeBasedRow.id()));
                assertThrows(IllegalStateException.class, () -> repository.checkInByQrCode("MEM-9999"));
                GymRepository.ActivityDemand demand = repository.activityDemand().stream()
                                .filter(row -> row.nameEnglish().equals(activity.nameEnglish()))
                                .findFirst().orElseThrow();
                assertEquals(2, demand.members());
                assertEquals(2, demand.checkIns());
                assertEquals(900, demand.revenue(), 0.001);

                repository.addScheduledClass("Test class", activity.id(), null, 1, "09:00", "10:00", 16);
                GymRepository.ScheduledClass scheduledClass = repository.scheduledClasses().stream()
                                .filter(row -> row.title().equals("Test class")).findFirst().orElseThrow();
                assertEquals(1, scheduledClass.dayOfWeek());
                repository.updateScheduledClass(scheduledClass.id(), "Updated class", activity.id(), null,
                                2, "10:00", "11:00", 20);
                assertEquals("Updated class", repository.scheduledClasses().stream()
                                .filter(row -> row.id() == scheduledClass.id()).findFirst().orElseThrow().title());
                assertTrue(repository.memberClasses(member.id()).stream()
                                .anyMatch(row -> row.title().equals("Updated class") && row.dayOfWeek() == 2));
                repository.deleteScheduledClass(scheduledClass.id());

                repository.addActivity(new Activity(0, "نشاط تجريبي", "Test activity", null));
                GymRepository.ActivityRow testActivity = repository.activities().stream()
                                .filter(row -> row.nameEnglish().equals("Test activity")).findFirst().orElseThrow();

                repository.addUser("test-reception", "securepass".toCharArray(), "Test Reception", "RECEPTIONIST",
                                "+201000000002", 5000);
                User staff = repository.users().stream()
                                .filter(row -> row.username().equals("test-reception"))
                                .findFirst().orElseThrow();
                YearMonth payMonth = YearMonth.now();
                repository.paySalary("USER", staff.id(), payMonth);
                GymRepository.Payroll staffPayroll = repository.payroll(payMonth).stream()
                                .filter(row -> row.employeeName().equals("Test Reception")).findFirst().orElseThrow();
                assertTrue(staffPayroll.paid());
                assertEquals(5000, staffPayroll.monthlySalary(), 0.001);
                assertThrows(SQLException.class, () -> repository.paySalary("USER", staff.id(), payMonth));
                repository.updateUser(staff.id(), "Updated Reception", "ADMIN", staff.phone(), 6000);
                assertTrue(repository.authenticate("test-reception", "securepass".toCharArray()).isPresent());
                repository.deactivateUser(staff.id());
                assertFalse(repository.authenticate("test-reception", "securepass".toCharArray()).isPresent());
                User admin = repository.users().stream()
                                .filter(row -> row.username().equals("admin"))
                                .findFirst().orElseThrow();
                assertThrows(SQLException.class, () -> repository.deactivateUser(admin.id()));

                repository.addCoach(new Coach(0, "Test Coach", "+201000000003", testActivity.id(), 8000, true));
                GymRepository.CoachRow coach = repository.coaches().stream()
                                .filter(row -> row.name().equals("Test Coach"))
                                .findFirst().orElseThrow();
                assertEquals(8000, coach.salary());
                repository.updateCoach(coach.id(), new Coach(coach.id(), "Updated Coach", coach.phone(),
                                testActivity.id(), 9000, false));
                assertEquals("Updated Coach", repository.coaches().stream()
                                .filter(row -> row.id() == coach.id()).findFirst().orElseThrow().name());
                repository.updateActivity(testActivity.id(),
                                new Activity(testActivity.id(), testActivity.nameArabic(), "Updated Activity", null));
                assertTrue(repository.activities().stream()
                                .anyMatch(row -> row.id() == testActivity.id()
                                                && row.nameEnglish().equals("Updated Activity")));
                repository.paySalary("COACH", coach.id(), payMonth);
                assertTrue(repository.payroll(payMonth).stream()
                                .filter(row -> row.employeeType().equals("COACH") && row.id() == coach.id())
                                .findFirst().orElseThrow().paid());
                repository.deleteCoach(coach.id());
                repository.deleteActivity(testActivity.id());
        }
}
