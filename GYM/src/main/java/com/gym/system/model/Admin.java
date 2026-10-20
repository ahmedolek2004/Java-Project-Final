package com.gym.system.model;

public final class Admin extends User {
    public Admin(int id, String username, String fullName, String phone, double salary) {
        super(id, username, fullName, "ADMIN", phone, salary);
    }
}
