package com.gym.system.model;

public final class Coach {
    private int id;
    private String name;
    private String phone;
    private Integer activityId;
    private double salary;
    private boolean personalTrainer;

    public Coach(int id, String name, String phone, Integer activityId, double salary, boolean personalTrainer) {
        this.id = id;
        setName(name);
        setPhone(phone);
        setActivityId(activityId);
        setSalary(salary);
        this.personalTrainer = personalTrainer;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public Integer getActivityId() { return activityId; }
    public double getSalary() { return salary; }
    public boolean isPersonalTrainer() { return personalTrainer; }
    public void setId(int id) { this.id = id; }
    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Coach name is required.");
        this.name = name.trim();
    }
    public void setPhone(String phone) { this.phone = phone == null || phone.isBlank() ? null : phone.trim(); }
    public void setActivityId(Integer activityId) { this.activityId = activityId; }
    public void setSalary(double salary) {
        if (!Double.isFinite(salary) || salary < 0) throw new IllegalArgumentException("Salary cannot be negative.");
        this.salary = salary;
    }
    public void setPersonalTrainer(boolean personalTrainer) { this.personalTrainer = personalTrainer; }
}
