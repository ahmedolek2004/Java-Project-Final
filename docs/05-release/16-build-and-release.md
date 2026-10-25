# 16. Build, Release & Deployment Guide

> **Document ID:** DOC-OPS-016  
> **Target Runtime:** Java 25 LTS / JavaFX 21.0.6  
> **Build Tool:** Apache Maven 3.9+  
> **Status:** Production-Ready

---

## 1. Prerequisites & Environment Setup

To compile and package the Forge Fitness desktop application from source:
1. **JDK 25:** Install an OpenJDK 25 distribution (e.g. Eclipse Temurin or Oracle OpenJDK 25).
2. **Environment Variable:** Ensure `JAVA_HOME` points to the JDK 25 root and `%JAVA_HOME%\bin` (or `$JAVA_HOME/bin`) is in `PATH`.
3. **Maven:** Maven 3.9+ (or use the included `mvnw` wrapper).

Verify environment:
```powershell
java -version
# openjdk version "25" ...
mvn -version
# Apache Maven 3.9+ ...
```

---

## 2. Compilation & Verification

### 2.1 Compile Source
```powershell
cd "GYM"
mvn clean compile
```

### 2.2 Run Automated Tests
```powershell
mvn test
```
All unit and integration tests will execute against in-memory SQLite and mock services.

### 2.3 Run Application in Development Mode
To launch the JavaFX application directly from Maven:
```powershell
mvn clean javafx:run
```

---

## 3. Production Packaging & Fat JAR Generation

### 3.1 Build Fat Executable JAR
To produce a standalone `.jar` including all class files, FXML resources, themes, and dependencies:
```powershell
mvn clean package -DskipTests
```
The resulting executable JAR will be located in:
```
GYM/target/gym-system-1.0-SNAPSHOT.jar
```

To run the fat JAR directly:
```powershell
java -jar target/gym-system-1.0-SNAPSHOT.jar
```
*(Note: `Launcher.java` is configured as the `Main-Class` manifest attribute, which prevents the JavaFX runtime initialization exception when launching via standard `java -jar`).*

---

## 4. Native Installer Creation via `jpackage`

To generate a native Windows `.msi` or `.exe` installer that bundles a minimal Java runtime (allowing clients to run Forge Fitness without pre-installing Java):

```powershell
jpackage --type msi `
  --name "ForgeFitness" `
  --app-version "1.0.0" `
  --input "target" `
  --main-jar "gym-system-1.0-SNAPSHOT.jar" `
  --main-class "com.gym.system.Launcher" `
  --win-shortcut `
  --win-menu `
  --vendor "Forge Martial Arts Academy"
```
Output: `ForgeFitness-1.0.0.msi` installer ready for client deployment.
