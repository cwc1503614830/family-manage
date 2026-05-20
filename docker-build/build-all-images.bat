@echo off

setlocal enabledelayedexpansion

cd /d "%~dp0"



set "EXIT_CODE=0"

set "PUSH=true"



:parse_flags

if "%~1"=="--help" goto :help

if "%~1"=="-h" goto :help

if "%~1"=="--no-push" (

  set "PUSH=false"

  shift

  goto :parse_flags

)

goto :main



:main

set "SCRIPT_DIR=%~dp0"

set "ROOT_DIR=%SCRIPT_DIR%.."

for %%I in ("%ROOT_DIR%") do set "ROOT_DIR=%%~fI"



set "BACKEND_DIR=%ROOT_DIR%\family-manage-backend"

set "FRONTEND_DIR=%ROOT_DIR%\family-mange-frontend"

set "BUILD_CONTEXT_DIR=%SCRIPT_DIR%build-context"



rem Harbor HTTP registry (no https:// prefix)

set "HARBOR_REGISTRY=192.168.216.140:8091"

set "HARBOR_PROJECT=family-manage"

set "HARBOR_USER=admin"

set "HARBOR_PASSWORD=Harbor12345"



if exist "%SCRIPT_DIR%.env" call :load_env "%SCRIPT_DIR%.env"

call :normalize_registry



set "TAG=%~1"

if "%TAG%"=="" set "TAG=latest"



set "BACKEND_REPO=%~2"

if "%BACKEND_REPO%"=="" set "BACKEND_REPO=family-manage-backend"



set "FRONTEND_REPO=%~3"

if "%FRONTEND_REPO%"=="" set "FRONTEND_REPO=family-manage-frontend"



set "BACKEND_IMAGE=%HARBOR_REGISTRY%/%HARBOR_PROJECT%/%BACKEND_REPO%"

set "FRONTEND_IMAGE=%HARBOR_REGISTRY%/%HARBOR_PROJECT%/%FRONTEND_REPO%"



echo.

echo ============================================================

echo Family Manage Docker image build

echo Root:      %ROOT_DIR%

echo Registry:  %HARBOR_REGISTRY%  (HTTP, not HTTPS)

echo Project:   %HARBOR_PROJECT%

echo Tag:       %TAG%

echo Backend:   %BACKEND_IMAGE%:%TAG%

echo Frontend:  %FRONTEND_IMAGE%:%TAG%

echo Push:      %PUSH%

echo ============================================================

echo.



where docker >nul 2>nul

if errorlevel 1 (

  echo [ERROR] docker was not found in PATH.

  call :fail 1

)



if not exist "%BACKEND_DIR%\pom.xml" (

  echo [ERROR] Backend project not found: %BACKEND_DIR%

  call :fail 1

)



if not exist "%FRONTEND_DIR%\package.json" (

  echo [ERROR] Frontend project not found: %FRONTEND_DIR%

  call :fail 1

)



echo [1/6] Building backend jar...

pushd "%BACKEND_DIR%"

call mvn -q -DskipTests package

if errorlevel 1 (

  popd

  echo [ERROR] Backend Maven build failed.

  call :fail 1

)

popd



set "BACKEND_JAR=%BACKEND_DIR%\target\family-manage-backend-1.0.0.jar"

if not exist "%BACKEND_JAR%" (

  echo [ERROR] Backend jar not found: %BACKEND_JAR%

  call :fail 1

)



echo [2/6] Building frontend dist...

pushd "%FRONTEND_DIR%"

call npm run build

if errorlevel 1 (

  popd

  echo [ERROR] Frontend npm build failed.

  call :fail 1

)

popd



if not exist "%FRONTEND_DIR%\dist\index.html" (

  echo [ERROR] Frontend dist not found: %FRONTEND_DIR%\dist

  call :fail 1

)



echo [3/6] Preparing Docker build context...

if exist "%BUILD_CONTEXT_DIR%" rmdir /s /q "%BUILD_CONTEXT_DIR%"

mkdir "%BUILD_CONTEXT_DIR%\backend"

mkdir "%BUILD_CONTEXT_DIR%\frontend"



copy /y "%BACKEND_JAR%" "%BUILD_CONTEXT_DIR%\backend\app.jar" >nul

copy /y "%SCRIPT_DIR%Dockerfile.backend" "%BUILD_CONTEXT_DIR%\backend\Dockerfile" >nul



xcopy "%FRONTEND_DIR%\dist" "%BUILD_CONTEXT_DIR%\frontend\dist\" /e /i /y >nul

copy /y "%SCRIPT_DIR%Dockerfile.frontend" "%BUILD_CONTEXT_DIR%\frontend\Dockerfile" >nul

copy /y "%SCRIPT_DIR%nginx.conf" "%BUILD_CONTEXT_DIR%\frontend\nginx.conf" >nul



echo [4/6] Building backend image...

docker build -t "%BACKEND_IMAGE%:%TAG%" "%BUILD_CONTEXT_DIR%\backend"

if errorlevel 1 (

  echo [ERROR] Backend docker image build failed.

  call :fail 1

)



echo [5/6] Building frontend image...

docker build -t "%FRONTEND_IMAGE%:%TAG%" "%BUILD_CONTEXT_DIR%\frontend"

if errorlevel 1 (

  echo [ERROR] Frontend docker image build failed.

  call :fail 1

)



if /i not "%PUSH%"=="true" goto :done_no_push



echo [6/6] Logging in to Harbor (HTTP) and pushing images...

call :check_insecure_registry

echo %HARBOR_PASSWORD%| docker login "%HARBOR_REGISTRY%" -u "%HARBOR_USER%" --password-stdin

if errorlevel 1 (

  echo.

  echo [ERROR] Harbor login failed.

  echo.

  echo Harbor is HTTP-only. Docker must allow insecure registry:

  echo   1. Docker Desktop -^> Settings -^> Docker Engine

  echo   2. Add or merge this into JSON:

  echo        "insecure-registries": ["%HARBOR_REGISTRY%"]

  echo   3. Apply ^& Restart Docker

  echo.

  echo Reference file: %SCRIPT_DIR%docker-daemon.example.json

  echo Do NOT use https:// in HARBOR_REGISTRY or docker login URL.

  call :fail 1

)



docker push "%BACKEND_IMAGE%:%TAG%"

if errorlevel 1 (

  echo [ERROR] Failed to push %BACKEND_IMAGE%:%TAG%

  echo        Ensure project "%HARBOR_PROJECT%" exists in Harbor.

  call :fail 1

)



docker push "%FRONTEND_IMAGE%:%TAG%"

if errorlevel 1 (

  echo [ERROR] Failed to push %FRONTEND_IMAGE%:%TAG%

  call :fail 1

)



echo.

echo [OK] Images built and pushed to Harbor:

echo   %BACKEND_IMAGE%:%TAG%

echo   %FRONTEND_IMAGE%:%TAG%

echo.

goto :finish_ok



:done_no_push

echo.

echo [OK] Images built locally (push skipped):

echo   %BACKEND_IMAGE%:%TAG%

echo   %FRONTEND_IMAGE%:%TAG%

echo.

goto :finish_ok



:normalize_registry

set "R=%HARBOR_REGISTRY%"

set "R=%R:https://=%"

set "R=%R:http://=%"

if "%R:~-1%"=="\" set "R=%R:~0,-1%"

if "%R:~-1%"=="/" set "R=%R:~0,-1%"

set "HARBOR_REGISTRY=%R%"

exit /b 0



:check_insecure_registry

docker info 2>nul | findstr /i /c:"%HARBOR_REGISTRY%" >nul 2>&1

if errorlevel 1 (

  echo [WARN] %HARBOR_REGISTRY% not found in "docker info" insecure registries.

  echo        If login fails, configure docker-daemon.example.json in Docker Desktop.

  echo.

)

exit /b 0



:load_env

if not exist "%~1" exit /b 0

for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%~1") do (

  if not "%%~a"=="" set "%%~a=%%~b"

)

exit /b 0



:fail

set "EXIT_CODE=%~1"

goto :finish



:finish_ok

set "EXIT_CODE=0"

goto :finish



:finish

echo.

if not "%EXIT_CODE%"=="0" (

  echo [FAILED] Build exited with code %EXIT_CODE%.

) else (

  echo [DONE] Finished successfully.

)

if /i not "%NO_PAUSE%"=="1" (

  echo Press any key to close this window...

  pause >nul

)

exit /b %EXIT_CODE%



:help

echo Usage:

echo   build-all-images.bat [--no-push] [tag] [backend-repo] [frontend-repo]

echo.

echo Harbor uses HTTP. Registry must be host:port only, e.g. 192.168.216.140:8091

echo Configure Docker Desktop -^> Docker Engine -^> insecure-registries

echo See: docker-daemon.example.json

echo.

if /i not "%NO_PAUSE%"=="1" pause

exit /b 0

