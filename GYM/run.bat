@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME (
  echo ERROR: JAVA_HOME is not set. Install JDK 21 or newer and set JAVA_HOME.
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: JAVA_HOME does not point to a valid JDK: %JAVA_HOME%
  exit /b 1
)
for /f "tokens=3" %%v in ('"%JAVA_HOME%\bin\java.exe" -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%~v"
for /f "tokens=1 delims=.\-_+" %%v in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%v"
if not defined JAVA_MAJOR (
  echo ERROR: Could not determine Java version. JDK 21 or newer is required.
  exit /b 1
)
if %JAVA_MAJOR% LSS 21 (
  echo ERROR: JDK 21 or newer is required. Detected Java %JAVA_VERSION%.
  exit /b 1
)
call mvnw.cmd -U javafx:run
exit /b %ERRORLEVEL%
