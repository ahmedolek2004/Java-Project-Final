package com.gym.system.model;

public abstract class User {
    private final int id;
    private final String username;
    private String fullName;
    private final String role;
    private String phone;
    private double salary;

    protected User(int id, String username, String fullName, String role, String phone, double salary) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.phone = phone;
        this.salary = salary;
    }

    public int id() { return id; }
    public String username() { return username; }
    public String fullName() { return fullName; }
    public String role() { return role; }
    public String phone() { return phone; }
    public double salary() { return salary; }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getPhone() { return phone; }
    public double getSalary() { return salary; }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required.");
        this.fullName = fullName.trim();
    }

    public void setPhone(String phone) {
        this.phone = phone == null || phone.isBlank() ? null : phone.trim();
    }

    public void setSalary(double salary) {
        if (!Double.isFinite(salary) || salary < 0) throw new IllegalArgumentException("Salary cannot be negative.");
        this.salary = salary;
    }
}
