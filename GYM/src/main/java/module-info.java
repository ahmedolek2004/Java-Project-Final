module com.gym.system {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires org.xerial.sqlitejdbc;
    requires com.google.zxing;
    opens com.gym.system.controller to javafx.fxml;
    exports com.gym.system;
    exports com.gym.system.model;
    exports com.gym.system.dao;
    exports com.gym.system.controller;
    exports com.gym.system.util;
}