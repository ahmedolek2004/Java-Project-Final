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
}