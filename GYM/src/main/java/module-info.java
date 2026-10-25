module com.gym.system {
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires transitive javafx.fxml;
    requires transitive java.sql;
    requires transitive com.google.zxing;
    requires java.desktop;
    requires java.prefs;

    opens com.gym.system to javafx.fxml;
    opens com.gym.system.controller to javafx.fxml;
    
    exports com.gym.system;
    exports com.gym.system.model;
    exports com.gym.system.dao;
    exports com.gym.system.controller;
    exports com.gym.system.controller.page;
    exports com.gym.system.controller.form;
    exports com.gym.system.controller.helper;
    exports com.gym.system.service;
    exports com.gym.system.util;
}