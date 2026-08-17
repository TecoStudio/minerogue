@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem Bootstrap the local test server on first run: create the folder,
rem download the Paper server jar, and accept the EULA.
if not exist "server" mkdir "server"
cd /d "server"

if not exist "server.jar" (
    echo Downloading Paper 1.21.11 server jar...
    for /f "usebackq delims=" %%u in (`powershell -NoProfile -Command "$r=Invoke-RestMethod 'https://fill.papermc.io/v3/projects/paper/versions/1.21.11/builds'; $f=$r | Select-Object -First 1; Write-Output $f.downloads.'server:default'.url"`) do set "PAPER_URL=%%u"
    if not defined PAPER_URL (
        echo Failed to resolve the Paper download URL. Check the network and try again.
        exit /b 1
    )
    curl.exe -f -L -o server.jar "%PAPER_URL%"
    if not exist "server.jar" (
        echo Failed to download the Paper server jar.
        exit /b 1
    )
)

if not exist "eula.txt" (
    echo eula=true>eula.txt
    echo Accepted the EULA.
)

rem Deploy the newest build: drop stale jars first so Paper/PlugManX
rem cannot load an older minerogue jar.
set "DEPLOYED="
for /f "delims=" %%f in ('dir /b /o-d /a-d "..\build\libs\minerogue-*.jar" 2^>nul') do (
    if not defined DEPLOYED set "DEPLOYED=%%f"
)
if defined DEPLOYED (
    del /q "plugins\minerogue-*.jar" 2>nul
    copy /y "..\build\libs\%DEPLOYED%" "plugins\" >nul
    echo Deployed newest build: plugins\%DEPLOYED%
) else (
    echo Warning: no build found at ..\build\libs\minerogue-*.jar, keeping existing plugins.
)

java -Xms1G -Xmx2G -jar server.jar --nogui
pause
