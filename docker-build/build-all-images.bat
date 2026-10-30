@echo off
setlocal enabledelayedexpansion

if "%~1"=="--help" goto :help
if "%~1"=="-h" goto :help

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
for %%I in ("%ROOT_DIR%") do set "ROOT_DIR=%%~fI"

set "BACKEND_DIR=%ROOT_DIR%\family-manage-backend"
set "FRONTEND_DIR=%ROOT_DIR%\family-mange-frontend"
set "BUILD_CONTEXT_DIR=%SCRIPT_DIR%build-context"

set "TAG=%~1"
if "%TAG%"=="" set "TAG=latest"

set "HARBOR_PROJECT=%~2"
if "%HARBOR_PROJECT%"=="" set "HARBOR_PROJECT=family-manage"

set "BACKEND_IMAGE=family-manage-backend"
set "FRONTEND_IMAGE=family-manage-frontend"

set "HARBOR_HOST=192.168.236.129:80"
if not "%HARBOR_HOST_OVERRIDE%"=="" set "HARBOR_HOST=%HARBOR_HOST_OVERRIDE%"

set "HARBOR_USER=admin"
if not "%HARBOR_USER_OVERRIDE%"=="" set "HARBOR_USER=%HARBOR_USER_OVERRIDE%"

set "HARBOR_PASS=Harbor12345"
if not "%HARBOR_PASS_OVERRIDE%"=="" set "HARBOR_PASS=%HARBOR_PASS_OVERRIDE%"

set "REMOTE_BACKEND=%HARBOR_HOST%/%HARBOR_PROJECT%/family-manage-backend"
set "REMOTE_FRONTEND=%HARBOR_HOST%/%HARBOR_PROJECT%/family-manage-frontend"

rem Default mirror: docker.1ms.run (verified by test-mirrors.bat on this network)
rem Switch mirror: set DOCKER_MIRROR=docker.m.daocloud.io
rem Use Docker Hub directly: set DOCKER_MIRROR=
if not defined DOCKER_MIRROR set "DOCKER_MIRROR=docker.1ms.run"
set "BACKEND_BASE=%DOCKER_MIRROR%/library/eclipse-temurin:8-jre"
set "FRONTEND_BASE=%DOCKER_MIRROR%/library/nginx:1.27-alpine"
if "%DOCKER_MIRROR%"=="" (
  set "BACKEND_BASE=eclipse-temurin:8-jre"
  set "FRONTEND_BASE=nginx:1.27-alpine"
)

echo.
echo ============================================================
echo Family Manage - Build and Push to Harbor
echo Root:     %ROOT_DIR%
echo Tag:      %TAG%
echo Mirror:   %DOCKER_MIRROR%
echo Backend:  %BACKEND_BASE%
echo Frontend: %FRONTEND_BASE%
echo Harbor:   %HARBOR_HOST%/%HARBOR_PROJECT%
echo.
echo One build serves ALL environments (prd / uat / dev):
echo   same backend jar ^(contains application-prd/uat/dev.properties^)
echo   same frontend dist ^(backend upstream injected by deploy script^)
echo   differences ^(profile / ports / container names^) come from the deploy scripts
echo Remote:   %REMOTE_BACKEND%:%TAG%
echo           %REMOTE_FRONTEND%:%TAG%
echo ============================================================
echo.
echo NOTE: HTTP Harbor requires insecure-registries in Docker Desktop:
echo   "insecure-registries": ["192.168.236.129:80"]
echo   Login/push must use host with port: %HARBOR_HOST%
echo.

where docker >nul 2>nul
if errorlevel 1 (
  echo [ERROR] docker was not found in PATH.
  goto :fail
)

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] java was not found in PATH.
  echo         Install JDK 8 and add %%JAVA_HOME%%\bin to PATH, then open a NEW cmd window.
  goto :fail
)

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] mvn was not found in PATH.
  echo         Install Maven and add its bin directory to PATH, then open a NEW cmd window.
  goto :fail
)

if not defined JAVA_HOME (
  echo [WARN] JAVA_HOME is not set. Maven may fail if java is not on PATH.
) else (
  if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JAVA_HOME is invalid: %JAVA_HOME%
    echo         java.exe not found under %%JAVA_HOME%%\bin
    goto :fail
  )
)

if not exist "%BACKEND_DIR%\pom.xml" (
  echo [ERROR] Backend project not found: %BACKEND_DIR%
  goto :fail
)

if not exist "%FRONTEND_DIR%\package.json" (
  echo [ERROR] Frontend project not found: %FRONTEND_DIR%
  goto :fail
)

echo [1/10] Pre-pulling base images (with mirror fallback)...
call :pull_base "%BACKEND_BASE%" "eclipse-temurin:8-jre" || goto :fail
call :pull_base "%FRONTEND_BASE%" "nginx:1.27-alpine" || goto :fail

echo [2/10] Building backend jar...
echo       (first run may download Maven dependencies, please wait...)
pushd "%BACKEND_DIR%"
call mvn -DskipTests package
if errorlevel 1 (
  popd
  echo [ERROR] Backend Maven build failed.
  goto :fail
)
popd

set "BACKEND_JAR=%BACKEND_DIR%\target\family-manage-backend-1.0.0.jar"
if not exist "%BACKEND_JAR%" (
  echo [ERROR] Backend jar not found: %BACKEND_JAR%
  goto :fail
)

echo [3/10] Building frontend dist...
pushd "%FRONTEND_DIR%"
call npm run build
if errorlevel 1 (
  popd
  echo [ERROR] Frontend npm build failed.
  goto :fail
)
popd

if not exist "%FRONTEND_DIR%\dist\index.html" (
  echo [ERROR] Frontend dist not found: %FRONTEND_DIR%\dist
  goto :fail
)

echo [4/10] Preparing Docker build context...
if exist "%BUILD_CONTEXT_DIR%" rmdir /s /q "%BUILD_CONTEXT_DIR%"
mkdir "%BUILD_CONTEXT_DIR%\backend"
mkdir "%BUILD_CONTEXT_DIR%\frontend"

copy /y "%BACKEND_JAR%" "%BUILD_CONTEXT_DIR%\backend\app.jar" >nul
copy /y "%SCRIPT_DIR%Dockerfile.backend" "%BUILD_CONTEXT_DIR%\backend\Dockerfile" >nul

xcopy "%FRONTEND_DIR%\dist" "%BUILD_CONTEXT_DIR%\frontend\dist\" /e /i /y >nul
copy /y "%SCRIPT_DIR%Dockerfile.frontend" "%BUILD_CONTEXT_DIR%\frontend\Dockerfile" >nul
rem nginx 模板由容器启动时 envsubst 渲染，后端容器名按环境注入（见 Dockerfile.frontend）
copy /y "%SCRIPT_DIR%nginx.conf.template" "%BUILD_CONTEXT_DIR%\frontend\nginx.conf.template" >nul

echo [5/10] Building backend image...
echo       Base image: %BACKEND_BASE%
set DOCKER_BUILDKIT=0
docker build --build-arg BASE_IMAGE=%BACKEND_BASE% -t "%BACKEND_IMAGE%:%TAG%" "%BUILD_CONTEXT_DIR%\backend"
if errorlevel 1 (
  echo [ERROR] Backend docker image build failed.
  goto :fail
)

echo [6/10] Building frontend image...
echo       Base image: %FRONTEND_BASE%
docker build --build-arg BASE_IMAGE=%FRONTEND_BASE% -t "%FRONTEND_IMAGE%:%TAG%" "%BUILD_CONTEXT_DIR%\frontend"
if errorlevel 1 (
  echo [ERROR] Frontend docker image build failed.
  goto :fail
)

echo [7/10] Logging in to Harbor...
echo %HARBOR_PASS%| docker login %HARBOR_HOST% -u %HARBOR_USER% --password-stdin
if errorlevel 1 (
  echo [ERROR] Harbor login failed.
  echo         Check host, credentials, and insecure-registries setting.
  goto :fail
)

echo [8/10] Tagging images for Harbor...
docker tag "%BACKEND_IMAGE%:%TAG%" "%REMOTE_BACKEND%:%TAG%"
if errorlevel 1 goto :fail
docker tag "%FRONTEND_IMAGE%:%TAG%" "%REMOTE_FRONTEND%:%TAG%"
if errorlevel 1 goto :fail

echo [9/10] Pushing backend image...
docker push "%REMOTE_BACKEND%:%TAG%"
if errorlevel 1 (
  echo [ERROR] Backend push failed.
  echo         Ensure Harbor project "%HARBOR_PROJECT%" exists and you have push permission.
  goto :fail
)

echo [10/10] Pushing frontend image...
docker push "%REMOTE_FRONTEND%:%TAG%"
if errorlevel 1 (
  echo [ERROR] Frontend push failed.
  goto :fail
)

echo.
echo [OK] Build and push completed:
echo   %REMOTE_BACKEND%:%TAG%
echo   %REMOTE_FRONTEND%:%TAG%
echo.
echo On Linux server (192.168.236.129), pick the environment you want to update:
echo   cd /data/project/family-manage/docker-build
echo   ./deploy-prd-from-harbor.sh %TAG%      ^(prd: internal,  ports 8091 / 12300^)
echo   ./deploy-uat-from-harbor.sh %TAG%      ^(uat: external,  ports 8092 / 12301^)
echo   ./deploy-dev-from-harbor.sh %TAG%      ^(dev: internal,  ports 8093 / 12302^)
echo.
echo Done.
rem 自动化/CI 场景可不按任意键退出：set NO_PAUSE=1
if not defined NO_PAUSE pause >nul
exit /b 0

:pull_base
rem Usage: call :pull_base <full_image> <standard_name>
rem e.g. call :pull_base "docker.1ms.run/library/eclipse-temurin:8-jre" "eclipse-temurin:8-jre"
set "PULL_IMAGE=%~1"
set "PULL_STANDARD=%~2"

docker image inspect "%PULL_STANDARD%" >nul 2>nul
if not errorlevel 1 (
    docker tag "%PULL_STANDARD%" "%PULL_IMAGE%" 2>nul
    echo       [SKIP] %PULL_STANDARD% already exists locally, tagged as %PULL_IMAGE%
    exit /b 0
)

echo       Pulling %PULL_IMAGE% ...
docker pull "%PULL_IMAGE%"
if not errorlevel 1 (
    docker tag "%PULL_IMAGE%" "%PULL_STANDARD%" 2>nul
    echo       [OK] Pulled and tagged as %PULL_STANDARD%
    exit /b 0
)

echo       [WARN] Failed from %PULL_IMAGE%, trying fallback mirrors ...

rem Fallback mirrors list — only verified mirrors on this network, ordered by speed test result
rem  (docker.nju.edu.cn 403, mirror.ccs.tencentyun.com/hub-mirror.c.163.com DNS fail — removed 2026-08-29)
set "FALLBACK_MIRRORS=docker.m.daocloud.io docker.xuanyuan.me docker.1panel.live docker.1ms.run"

for %%M in (%FALLBACK_MIRRORS%) do (
    echo       Trying %%M/library/!PULL_STANDARD! ...
    docker pull "%%M/library/!PULL_STANDARD!"
    if not errorlevel 1 (
        docker tag "%%M/library/!PULL_STANDARD!" "!PULL_STANDARD!" 2>nul
        docker tag "%%M/library/!PULL_STANDARD!" "!PULL_IMAGE!" 2>nul
        echo       [OK] Pulled from %%M and tagged as !PULL_STANDARD!
        exit /b 0
    )
    echo       [SKIP] %%M failed, continuing...
)

echo       [ERROR] All mirrors failed for %PULL_STANDARD%
echo       Please check network, or set DOCKER_MIRROR to an accessible mirror
exit /b 1

:fail
echo.
echo Build or push failed.
if not defined NO_PAUSE pause >nul
exit /b 1

:help
echo Usage:
echo   build-all-images.bat [tag] [harbor-project]
echo.
echo Defaults:
echo   tag            latest
echo   harbor-project family-manage
echo.
echo Environment overrides:
echo   DOCKER_MIRROR, HARBOR_HOST_OVERRIDE, HARBOR_USER_OVERRIDE, HARBOR_PASS_OVERRIDE
echo.
echo Mirrors VERIFIED on your network (last test 2026-08-29, sorted by speed):
echo   docker.1ms.run            (default, fastest on your test)
echo   docker.m.daocloud.io      (Daocloud, fallback 1)
echo   docker.xuanyuan.me         (Xuanyuan, fallback 2)
echo   docker.1panel.live         (1Panel, fallback 3)
echo.
echo Known unavailable on your network (skipped):
echo   docker.nju.edu.cn         (403 Forbidden)
echo   mirror.ccs.tencentyun.com  (DNS: no such host)
echo   hub-mirror.c.163.com       (DNS: no such host)
echo.
echo Examples:
echo   build-all-images.bat v1.0.0 family-manage
echo   set DOCKER_MIRROR=docker.m.daocloud.io ^&^& build-all-images.bat
echo.
echo Before building, you can run test-mirrors.bat to find the fastest mirror.
exit /b 0
