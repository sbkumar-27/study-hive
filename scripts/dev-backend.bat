@echo off
REM Runs the Spring Boot backend in dev mode. Requires .env (copy from
REM .env.example) and a running MySQL80 service.
setlocal enabledelayedexpansion
cd /d "%~dp0\.."

if not exist ".env" (
    echo [dev-backend] .env not found. Run: copy .env.example .env
    exit /b 1
)

for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
    set "%%A=%%B"
)

cd backend
mvn spring-boot:run
