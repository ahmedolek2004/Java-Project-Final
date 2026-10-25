# GYM Management System - Project Structure Guide

This document outlines the project's directory structure, architectural layers, and guidelines for maintaining clean code when adding or removing features.

## Directory Tree & Packages
- `com.gym.system`
  - `GymApplication.java` -> Main entry point and lifecycle manager.
  - `Launcher.java` -> Helper launcher class for JavaFX fat-jar compatibility.
  - `controller/` -> Handles UI interactions and FXML event bindings.
    - `DashboardController.java`
    - `LoginController.java`
    - `PrintController.java`
    - `FormDialog.java`
  - `dao/` -> Data Access Objects and Repository pattern implementation (Database operations).
    - `GymDataAccess.java` (Interface)
    - `GymRepository.java` (Implementation)
    - `DatabaseConnection.java`
  - `model/` -> Plain Old Java Objects (POJO) representing database entities.
    - `Member.java`, `User.java`, `Coach.java`, `Subscription.java`, `Activity.java`, etc.
  - `util/` -> Helper and service classes (Localization, Printing, QR codes, Alerts, Styling).
    - `LanguageManager.java`, `QrCodeService.java`, `AppearanceSettings.java`, etc.
- `resources/com/gym/system/`
  - `views/fxml/` -> FXML layout files (`dashboard.fxml`, `login.fxml`).
  - `views/css/` -> Stylesheets (`style.css`).
  - `messages_ar.properties`, `messages_en.properties` -> Localization bundles.

## Guidelines for Modifying Structure
1. **Adding a New Entity/Table:**
   - Create the model class inside `model/`.
   - Create or update the DAO/Repository methods in `dao/` to handle SQL queries.
   - Add corresponding UI views in `views/fxml/` and controllers in `controller/`.
2. **Refactoring Code:**
   - Keep business logic out of UI controllers; delegate queries and calculations to the `dao` layer.
   - Keep `GymApplication.java` lean by extracting modular page builders into separate helper classes or controllers if it grows too large.