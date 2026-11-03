package com.gym.system.dao;

import com.gym.system.model.Member;
import com.gym.system.model.Subscription;

import java.sql.SQLException;
import java.util.List;

public interface MemberDAO {
    List<GymRepository.MemberRow> members() throws SQLException;
    void addMember(Member member) throws SQLException;
    void updateMember(int id, Member member) throws SQLException;
    void deleteMember(int id) throws SQLException;
    List<GymRepository.MemberSubscription> memberSubscriptions(int memberId) throws SQLException;
    int registerMember(Member member, Subscription subscription, boolean paidNow) throws SQLException;
    List<GymRepository.ClassSeat> availableClasses(Integer includeClassId) throws SQLException;
    List<GymRepository.MemberVisit> memberVisits(int memberId, int limit) throws SQLException;
}
