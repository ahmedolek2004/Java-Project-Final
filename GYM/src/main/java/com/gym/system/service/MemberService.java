package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository.MemberClass;
import com.gym.system.dao.GymRepository.MemberRow;
import com.gym.system.dao.GymRepository.MemberSubscription;
import com.gym.system.dao.GymRepository.MemberVisit;
import com.gym.system.model.Member;

import java.sql.SQLException;
import java.util.List;

/**
 * Member use cases. Medical notes are confidential: only administrators can read them. Reception can
 * record notes when registering a member but can neither read nor overwrite them afterwards.
 */
public class MemberService {
    private final GymDataAccess repository;
    private final AccessControl access;

    public MemberService(GymDataAccess repository, AccessControl access) {
        this.repository = repository;
        this.access = access;
    }

    public List<MemberRow> getAllMembers() throws SQLException {
        access.requireSignedIn();
        List<MemberRow> rows = repository.members();
        if (access.isAdmin()) {
            return rows;
        }
        return rows.stream().map(MemberService::withoutMedicalNotes).toList();
    }

    public List<MemberSubscription> getSubscriptions(int memberId) throws SQLException {
        access.requireSignedIn();
        return repository.memberSubscriptions(memberId);
    }

    public List<MemberClass> getClasses(int memberId) throws SQLException {
        access.requireSignedIn();
        return repository.memberClasses(memberId);
    }

    public List<MemberVisit> getRecentVisits(int memberId, int limit) throws SQLException {
        access.requireSignedIn();
        return repository.memberVisits(memberId, limit);
    }

    public void addMember(Member member) throws SQLException {
        access.requireSignedIn();
        repository.addMember(member);
    }

    public void updateMember(int id, Member member) throws SQLException {
        access.requireSignedIn();
        repository.updateMember(id, access.isAdmin() ? member : keepingStoredNotes(id, member));
    }

    private Member keepingStoredNotes(int id, Member member) throws SQLException {
        String storedNotes = repository.members().stream()
                .filter(row -> row.id() == id)
                .map(MemberRow::medicalNotes)
                .findFirst()
                .orElse(null);
        return new Member(member.getId(), member.getFullName(), member.getPhone(), member.getEmail(),
                member.getGender(), storedNotes);
    }

    private static MemberRow withoutMedicalNotes(MemberRow row) {
        return new MemberRow(row.id(), row.fullName(), row.phone(), row.email(), row.gender(),
                null, row.plan(), row.status());
    }
}
