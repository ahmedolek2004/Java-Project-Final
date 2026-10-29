package com.gym.system.dao;

import java.sql.SQLException;
import java.util.List;

/** Check-in and attendance operations. */
public interface AttendanceDAO {

    GymRepository.CheckInResult checkIn(int memberId) throws SQLException;

    GymRepository.CheckInResult checkInByQrCode(String value) throws SQLException;

    List<GymRepository.Attendance> attendance() throws SQLException;
}
