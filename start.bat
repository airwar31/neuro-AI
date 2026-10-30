@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo =====================================================================
echo  NeuroAI Workstation - Clinical Diagnostic Application
echo =====================================================================
echo.

set "JAVA_CMD="
set "JAVAC_CMD="

:: 1. Check PrismLauncher OpenJDK
if exist "%APPDATA%\PrismLauncher\java\java-runtime-epsilon\bin\java.exe" (
    set "JAVA_CMD=%APPDATA%\PrismLauncher\java\java-runtime-epsilon\bin\java.exe"
    if exist "%APPDATA%\PrismLauncher\java\java-runtime-epsilon\bin\javac.exe" (
        set "JAVAC_CMD=%APPDATA%\PrismLauncher\java\java-runtime-epsilon\bin\javac.exe"
    )
)

:: 2. Check system PATH
if "%JAVA_CMD%"=="" (
    where java >nul 2>nul
    if not errorlevel 1 (
        set "JAVA_CMD=java"
        set "JAVAC_CMD=javac"
    )
)

:: 3. Check Program Files
if "%JAVA_CMD%"=="" (
    for /d %%J in ("%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles%\Microsoft\*") do (
        if exist "%%~J\bin\java.exe" (
            set "JAVA_CMD=%%~J\bin\java.exe"
            if exist "%%~J\bin\javac.exe" (
                set "JAVAC_CMD=%%~J\bin\javac.exe"
            )
        )
    )
)

if "%JAVA_CMD%"=="" (
    echo [ОШИБКА] Среда выполнения Java не обнаружена в системе.
    echo Пожалуйста, установите Java OpenJDK 17 или выше.
    pause
    exit /b 1
)

:: Compile sources if javac exists
if not "%JAVAC_CMD%"=="" (
    if exist "%JAVAC_CMD%" (
        echo [INFO] Компиляция модулей графического интерфейса Java...
        if not exist "desktop\build\classes" mkdir "desktop\build\classes"
        "%JAVAC_CMD%" -encoding UTF-8 -d "desktop\build\classes" desktop\src\com\neuroai\desktop\*.java
        if errorlevel 1 (
            echo [ПРЕДУПРЕЖДЕНИЕ] Сборка завершилась с предупреждениями. Запуск продолжается...
        )
    )
)

set PYTHONIOENCODING=utf-8
set PYTHONUTF8=1

echo [INFO] Запуск медицинского комплекса NeuroAI Workstation...
"%JAVA_CMD%" "-Dfile.encoding=UTF-8" -cp "desktop\build\classes;desktop\neuro-ai-app.jar" com.neuroai.desktop.NeuroApp

if errorlevel 1 (
    echo.
    echo [ОШИБКА] Приложение завершилось со сбоем.
    pause
)
