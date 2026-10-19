@echo off
REM Runs the JavaFX desktop client. Make sure the backend is already
REM running (scripts\dev-backend.bat) before launching this.
setlocal
cd /d "%~dp0\..\desktop"

mvn javafx:run
