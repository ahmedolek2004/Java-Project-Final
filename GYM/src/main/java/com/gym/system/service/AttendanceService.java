package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository.Attendance;
import com.gym.system.dao.GymRepository.CheckInResult;

import java.sql.SQLException;
import java.util.List;

public class AttendanceService {
    private final GymDataAccess repository;
    private final AccessControl access;

    public AttendanceService(GymDataAccess repository, AccessControl access) {
        this.repository = repository;
        this.access = access;
    }

    public List<Attendance> getAllAttendance() throws SQLException {
        access.requireSignedIn();
        return repository.attendance();
    }

    public CheckInResult checkIn(int memberId) throws SQLException {
        access.requireSignedIn();
        return repository.checkIn(memberId);
    }

    public CheckInResult checkInByQr(String qrCode) throws SQLException {
        return checkInByQrCode(qrCode);
    }

    public CheckInResult checkInByQrCode(String qrCode) throws SQLException {
        access.requireSignedIn();
        return repository.checkInByQrCode(qrCode);
    }
}
