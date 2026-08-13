@echo off
setlocal EnableExtensions

rem Create the local Paper test-server folder, then bootstrap and start it.
set "PROJECT_ROOT=%~dp0"
set "SERVER_DIR=%PROJECT_ROOT%server"
set "SERVER_SCRIPT=%SERVER_DIR%start.bat"

if not exist "%SERVER_DIR%\" mkdir "%SERVER_DIR%"
if not exist "%SERVER_SCRIPT%" (
    echo Missing test-server bootstrap: %SERVER_SCRIPT%
    echo Restore server\start.bat or ask the developer to recreate it.
    exit /b 1
)

call "%SERVER_SCRIPT%"
exit /b %errorlevel%
