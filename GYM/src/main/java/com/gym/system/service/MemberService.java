package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Member;
import java.sql.SQLException;
import java.util.List;

public class MemberService {
    private final GymDataAccess repository;

    public MemberService(GymDataAccess repository) {
        this.repository = repository;
    }

    public List<GymRepository.MemberRow> getAllMembers() throws SQLException {
        return repository.members();
    }

    public void addMember(Member member) throws SQLException {
        repository.addMember(member);
    }

    public void updateMember(int id, Member member) throws SQLException {
        repository.updateMember(id, member);
    }

    public List<GymRepository.MemberSubscription> getSubscriptions(int memberId) throws SQLException {
        return repository.memberSubscriptions(memberId);
    }

    public List<GymRepository.MemberClass> getClasses(int memberId) throws SQLException {
        return repository.memberClasses(memberId);
    }

    public List<GymRepository.MemberVisit> getRecentVisits(int memberId, int limit) throws SQLException {
        return repository.memberVisits(memberId, limit);
    }
}
