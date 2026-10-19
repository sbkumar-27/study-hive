@echo off
REM Runs the React web client in dev mode (Vite dev server, :5173).
setlocal
cd /d "%~dp0\..\web"

if not exist "node_modules" (
    echo [dev-web] node_modules not found, running npm install first...
    call npm install
    if errorlevel 1 exit /b 1
)

call npm run dev
