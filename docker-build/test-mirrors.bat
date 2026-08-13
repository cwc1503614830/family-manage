@echo off
setlocal enabledelayedexpansion

echo ============================================================
echo  Docker Mirror Speed Test
echo ============================================================
echo.

set "TEST_IMAGE=library/nginx:1.27-alpine"

set "MIRRORS=docker.1ms.run,docker.nju.edu.cn,docker.m.daocloud.io,docker.xuanyuan.me,docker.1panel.live,mirror.ccs.tencentyun.com,hub-mirror.c.163.com"

for /f "tokens=1-10 delims=, " %%A in ("%MIRRORS%") do (
    set "M1=%%A"
    set "M2=%%B"
    set "M3=%%C"
    set "M4=%%D"
    set "M5=%%E"
    set "M6=%%F"
    set "M7=%%G"
)

if not "!M1!"=="" call :test "!M1!"
if not "!M2!"=="" call :test "!M2!"
if not "!M3!"=="" call :test "!M3!"
if not "!M4!"=="" call :test "!M4!"
if not "!M5!"=="" call :test "!M5!"
if not "!M6!"=="" call :test "!M6!"
if not "!M7!"=="" call :test "!M7!"

echo.
echo ============================================================
echo  Test complete. Check above for fastest mirror.
echo ============================================================
echo.
echo To use the fastest mirror, set DOCKER_MIRROR before building:
echo   set DOCKER_MIRROR=FASTEST_MIRROR
echo   build-all-images.bat
echo.
pause
exit /b 0

:test
set "MIRROR=%~1"
echo ------------------------------------------------------------
echo Testing: %MIRROR% ...
echo.
docker pull "%MIRROR%/%TEST_IMAGE%" 2>&1
if errorlevel 1 (
    echo [FAIL] %MIRROR% - Cannot pull
) else (
    echo [OK]   %MIRROR% - SUCCESS
    docker rmi "%MIRROR%/%TEST_IMAGE%" 2>nul
)
echo.
exit /b 0
