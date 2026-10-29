package com.gym.system.dao;

import com.gym.system.model.Activity;
import com.gym.system.model.Coach;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

public interface GymDataAccess extends UserDAO, MemberDAO, SubscriptionDAO, AttendanceDAO, FinanceDAO, SettingsDAO {

    void addActivity(Activity activity) throws SQLException;

    void updateActivity(int id, Activity activity) throws SQLException;

    void deleteActivity(int id) throws SQLException;

    List<GymRepository.ActivityRow> activities() throws SQLException;

    void addCoach(Coach coach) throws SQLException;

    void updateCoach(int id, Coach coach) throws SQLException;

    void deleteCoach(int id) throws SQLException;

    List<GymRepository.CoachRow> coaches() throws SQLException;

    void addScheduledClass(String title, int activityId, Integer coachId, int dayOfWeek,
            String startTime, String endTime, int capacity) throws SQLException;

    void updateScheduledClass(int id, String title, int activityId, Integer coachId, int dayOfWeek,
            String startTime, String endTime, int capacity) throws SQLException;

    void deleteScheduledClass(int id) throws SQLException;

    List<GymRepository.ScheduledClass> scheduledClasses() throws SQLException;

    List<GymRepository.Payroll> payroll(YearMonth month) throws SQLException;

    void paySalary(String employeeType, int employeeId, YearMonth month) throws SQLException;

    List<GymRepository.ActivityDemand> activityDemand() throws SQLException;
}
