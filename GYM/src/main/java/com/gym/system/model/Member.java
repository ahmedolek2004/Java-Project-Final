package com.gym.system.model;

public final class Member {
    private int id;
    private String fullName;
    private String phone;
    private String email;
    private String gender;
    private String medicalNotes;

    public Member(int id, String fullName, String phone, String email, String gender, String medicalNotes) {
        this.id = id;
        setFullName(fullName);
        setPhone(phone);
        setEmail(email);
        setGender(gender);
        setMedicalNotes(medicalNotes);
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getGender() { return gender; }
    public String getMedicalNotes() { return medicalNotes; }

    public void setId(int id) { this.id = id; }
    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required.");
        this.fullName = fullName.trim();
    }
    public void setPhone(String phone) {
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("Phone is required.");
        this.phone = phone.trim();
    }
    public void setEmail(String email) { this.email = normalize(email); }
    public void setGender(String gender) { this.gender = normalize(gender); }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = normalize(medicalNotes); }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
