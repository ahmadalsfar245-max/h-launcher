@echo off
setlocal

set "GRADLE_VERSION=8.9"
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "GRADLE_HOME=%GRADLE_USER_HOME%\wrapper\dists\gradle-%GRADLE_VERSION%-bin\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat"
set "DIST_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_BIN%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    set "BOOTSTRAP_DIR=%TEMP%\hlauncher-gradle-%RANDOM%"
    mkdir "%BOOTSTRAP_DIR%"

    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$ProgressPreference = 'SilentlyContinue';" ^
        "Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile '%BOOTSTRAP_DIR%\gradle.zip';" ^
        "Expand-Archive -Path '%BOOTSTRAP_DIR%\gradle.zip' -DestinationPath '%BOOTSTRAP_DIR%\expanded' -Force"

    if errorlevel 1 (
        echo Error: Gradle could not be downloaded or extracted.
        exit /b 1
    )

    if not exist "%GRADLE_HOME%" mkdir "%GRADLE_HOME%"
    xcopy "%BOOTSTRAP_DIR%\expanded\gradle-%GRADLE_VERSION%\*" "%GRADLE_HOME%\" /E /I /Y >nul
    rmdir /S /Q "%BOOTSTRAP_DIR%"
)

call "%GRADLE_BIN%" %*
exit /b %ERRORLEVEL%