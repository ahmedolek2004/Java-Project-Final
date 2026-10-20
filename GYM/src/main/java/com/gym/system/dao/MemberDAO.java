package com.gym.system.dao;

import com.gym.system.model.Member;

import java.sql.SQLException;
import java.util.List;

public interface MemberDAO {
    List<GymRepository.MemberRow> members() throws SQLException;
    void addMember(Member member) throws SQLException;
    void updateMember(int id, Member member) throws SQLException;
}
