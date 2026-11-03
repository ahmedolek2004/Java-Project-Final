package com.gym.system.model;

public final class Receptionist extends User {
    public Receptionist(int id, String username, String fullName, String phone, double salary) {
        super(id, username, fullName, "RECEPTIONIST", phone, salary);
    }
}
