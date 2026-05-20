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

set "BACKEND_IMAGE=%~2"
if "%BACKEND_IMAGE%"=="" set "BACKEND_IMAGE=family-manage-backend"

set "FRONTEND_IMAGE=%~3"
if "%FRONTEND_IMAGE%"=="" set "FRONTEND_IMAGE=family-manage-frontend"

echo.
echo ============================================================
echo Family Manage Docker image build
echo Root:     %ROOT_DIR%
echo Tag:      %TAG%
echo Backend:  %BACKEND_IMAGE%:%TAG%
echo Frontend: %FRONTEND_IMAGE%:%TAG%
echo ============================================================
echo.

where docker >nul 2>nul
if errorlevel 1 (
  echo [ERROR] docker was not found in PATH.
  exit /b 1
)

if not exist "%BACKEND_DIR%\pom.xml" (
  echo [ERROR] Backend project not found: %BACKEND_DIR%
  exit /b 1
)

if not exist "%FRONTEND_DIR%\package.json" (
  echo [ERROR] Frontend project not found: %FRONTEND_DIR%
  exit /b 1
)

echo [1/5] Building backend jar...
pushd "%BACKEND_DIR%"
call mvn -q -DskipTests package
if errorlevel 1 (
  popd
  echo [ERROR] Backend Maven build failed.
  exit /b 1
)
popd

set "BACKEND_JAR=%BACKEND_DIR%\target\family-manage-backend-1.0.0.jar"
if not exist "%BACKEND_JAR%" (
  echo [ERROR] Backend jar not found: %BACKEND_JAR%
  exit /b 1
)

echo [2/5] Building frontend dist...
pushd "%FRONTEND_DIR%"
call npm run build
if errorlevel 1 (
  popd
  echo [ERROR] Frontend npm build failed.
  exit /b 1
)
popd

if not exist "%FRONTEND_DIR%\dist\index.html" (
  echo [ERROR] Frontend dist not found: %FRONTEND_DIR%\dist
  exit /b 1
)

echo [3/5] Preparing Docker build context...
if exist "%BUILD_CONTEXT_DIR%" rmdir /s /q "%BUILD_CONTEXT_DIR%"
mkdir "%BUILD_CONTEXT_DIR%\backend"
mkdir "%BUILD_CONTEXT_DIR%\frontend"

copy /y "%BACKEND_JAR%" "%BUILD_CONTEXT_DIR%\backend\app.jar" >nul
copy /y "%SCRIPT_DIR%Dockerfile.backend" "%BUILD_CONTEXT_DIR%\backend\Dockerfile" >nul

xcopy "%FRONTEND_DIR%\dist" "%BUILD_CONTEXT_DIR%\frontend\dist\" /e /i /y >nul
copy /y "%SCRIPT_DIR%Dockerfile.frontend" "%BUILD_CONTEXT_DIR%\frontend\Dockerfile" >nul
copy /y "%SCRIPT_DIR%nginx.conf" "%BUILD_CONTEXT_DIR%\frontend\nginx.conf" >nul

echo [4/5] Building backend image...
docker build -t "%BACKEND_IMAGE%:%TAG%" "%BUILD_CONTEXT_DIR%\backend"
if errorlevel 1 (
  echo [ERROR] Backend docker image build failed.
  exit /b 1
)

echo [5/5] Building frontend image...
docker build -t "%FRONTEND_IMAGE%:%TAG%" "%BUILD_CONTEXT_DIR%\frontend"
if errorlevel 1 (
  echo [ERROR] Frontend docker image build failed.
  exit /b 1
)

echo.
echo [OK] Images built successfully:
echo   %BACKEND_IMAGE%:%TAG%
echo   %FRONTEND_IMAGE%:%TAG%
echo.
echo Example run:
echo   docker network create family-manage-net
echo   docker run -d --name family-manage-backend --network family-manage-net -p 12700:12700 %BACKEND_IMAGE%:%TAG%
echo   docker run -d --name family-manage-frontend --network family-manage-net -p 8080:80 %FRONTEND_IMAGE%:%TAG%
echo.
exit /b 0

:help
echo Usage:
echo   build-all-images.bat [tag] [backend-image-name] [frontend-image-name]
echo.
echo Defaults:
echo   tag                 latest
echo   backend-image-name  family-manage-backend
echo   frontend-image-name family-manage-frontend
echo.
echo Example:
echo   build-all-images.bat v1.0.0 myrepo/family-manage-backend myrepo/family-manage-frontend
exit /b 0
