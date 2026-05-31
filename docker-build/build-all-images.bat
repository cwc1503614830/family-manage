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

rem Override with empty value to use Docker Hub directly: set DOCKER_MIRROR=
if not defined DOCKER_MIRROR set "DOCKER_MIRROR=docker.m.daocloud.io"
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
echo Local:    %BACKEND_IMAGE%:%TAG%, %FRONTEND_IMAGE%:%TAG%
echo Harbor:   %HARBOR_HOST%/%HARBOR_PROJECT%
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

echo [1/9] Building backend jar...
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

echo [2/9] Building frontend dist...
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

echo [3/9] Preparing Docker build context...
if exist "%BUILD_CONTEXT_DIR%" rmdir /s /q "%BUILD_CONTEXT_DIR%"
mkdir "%BUILD_CONTEXT_DIR%\backend"
mkdir "%BUILD_CONTEXT_DIR%\frontend"

copy /y "%BACKEND_JAR%" "%BUILD_CONTEXT_DIR%\backend\app.jar" >nul
copy /y "%SCRIPT_DIR%Dockerfile.backend" "%BUILD_CONTEXT_DIR%\backend\Dockerfile" >nul

xcopy "%FRONTEND_DIR%\dist" "%BUILD_CONTEXT_DIR%\frontend\dist\" /e /i /y >nul
copy /y "%SCRIPT_DIR%Dockerfile.frontend" "%BUILD_CONTEXT_DIR%\frontend\Dockerfile" >nul
copy /y "%SCRIPT_DIR%nginx.conf" "%BUILD_CONTEXT_DIR%\frontend\nginx.conf" >nul

echo [4/9] Building backend image...
echo       Base image: %BACKEND_BASE%
docker build --build-arg BASE_IMAGE=%BACKEND_BASE% -t "%BACKEND_IMAGE%:%TAG%" "%BUILD_CONTEXT_DIR%\backend"
if errorlevel 1 (
  echo [ERROR] Backend docker image build failed.
  echo         Try another mirror: set DOCKER_MIRROR=docker.1ms.run
  goto :fail
)

echo [5/9] Building frontend image...
echo       Base image: %FRONTEND_BASE%
docker build --build-arg BASE_IMAGE=%FRONTEND_BASE% -t "%FRONTEND_IMAGE%:%TAG%" "%BUILD_CONTEXT_DIR%\frontend"
if errorlevel 1 (
  echo [ERROR] Frontend docker image build failed.
  goto :fail
)

echo [6/9] Logging in to Harbor...
echo %HARBOR_PASS%| docker login %HARBOR_HOST% -u %HARBOR_USER% --password-stdin
if errorlevel 1 (
  echo [ERROR] Harbor login failed.
  echo         Check host, credentials, and insecure-registries setting.
  goto :fail
)

echo [7/9] Tagging images for Harbor...
docker tag "%BACKEND_IMAGE%:%TAG%" "%REMOTE_BACKEND%:%TAG%"
if errorlevel 1 goto :fail
docker tag "%FRONTEND_IMAGE%:%TAG%" "%REMOTE_FRONTEND%:%TAG%"
if errorlevel 1 goto :fail

echo [8/9] Pushing backend image...
docker push "%REMOTE_BACKEND%:%TAG%"
if errorlevel 1 (
  echo [ERROR] Backend push failed.
  echo         Ensure Harbor project "%HARBOR_PROJECT%" exists and you have push permission.
  goto :fail
)

echo [9/9] Pushing frontend image...
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
echo On Linux server, run:
echo   chmod +x deploy-from-harbor.sh
echo   ./deploy-from-harbor.sh %TAG% %HARBOR_PROJECT%
echo.
echo Done. Press any key to close...
pause >nul
exit /b 0

:fail
echo.
echo Build or push failed. Press any key to close...
pause >nul
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
echo Example:
echo   build-all-images.bat v1.0.0 family-manage
exit /b 0
