package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import java.sql.SQLException;
import java.util.List;

public class AttendanceService {
    private final GymDataAccess repository;
    public AttendanceService(GymDataAccess repository) { this.repository = repository; }

    public List<GymRepository.Attendance> getAllAttendance() throws SQLException {
        return repository.attendance();
    }
    public GymRepository.CheckInResult checkIn(int memberId) throws SQLException {
        return repository.checkIn(memberId);
    }
    public GymRepository.CheckInResult checkInByQr(String qrCode) throws SQLException {
        return repository.checkInByQrCode(qrCode);
    }
    public GymRepository.CheckInResult checkInByQrCode(String qrCode) throws SQLException {
        return repository.checkInByQrCode(qrCode);
    }
}