@echo off
setlocal

set "ROOT=%~dp0.."
set "GDAL4ANDROID=%ROOT%\third_party\GDAL4Android"
set "MSYS2_BASH=C:\msys64\usr\bin\bash.exe"
if not exist "%MSYS2_BASH%" set "MSYS2_BASH=C:\msys64full\usr\bin\bash.exe"

if not exist "%MSYS2_BASH%" (
  echo MSYS2 not found. Installing via winget...
  winget install --id MSYS2.MSYS2 -e --accept-source-agreements --accept-package-agreements
  if errorlevel 1 exit /b 1
  if not exist "%MSYS2_BASH%" (
    if exist "C:\msys64full\usr\bin\bash.exe" (
      set "MSYS2_BASH=C:\msys64full\usr\bin\bash.exe"
    ) else (
      echo MSYS2 install did not create a usable bash executable.
      echo Expected either C:\msys64\usr\bin\bash.exe or C:\msys64full\usr\bin\bash.exe
      exit /b 1
    )
  )
)

if not exist "%GDAL4ANDROID%\gdal\build.gradle" (
  echo GDAL4Android source not found at %GDAL4ANDROID%
  exit /b 1
)

echo [1/5] Installing MSYS2 build tools...
"%MSYS2_BASH%" -lc "pacman -Sy --noconfirm --needed wget make cmake ninja tar gzip patch autoconf automake libtool gawk gettext"
if errorlevel 1 exit /b 1

echo [2/5] Building GDAL AAR (this can take a while)...
pushd "%GDAL4ANDROID%"
set "PATH=%LOCALAPPDATA%\Android\Sdk\cmake\3.22.1\bin;C:\msys64\usr\bin;%PATH%"
call gradlew gdal:assembleRelease
if errorlevel 1 (
  popd
  exit /b 1
)
popd

set "SRC_AAR=%GDAL4ANDROID%\gdal\build\outputs\aar\gdal-release.aar"
set "DST_AAR=%ROOT%\app\src\main\libs\gdal-debug-16kb.aar"

if not exist "%SRC_AAR%" (
  echo Built AAR not found: %SRC_AAR%
  exit /b 1
)

echo [3/5] Installing drop-in AAR...
copy /Y "%SRC_AAR%" "%DST_AAR%" >nul
if errorlevel 1 exit /b 1

echo [4/5] Verifying AAR alignment...
pushd "%ROOT%"
call gradlew :app:verify16KbGdalAar
if errorlevel 1 (
  popd
  exit /b 1
)

echo [5/5] Verifying APK alignment...
call gradlew :app:verify16KbDebug
if errorlevel 1 (
  popd
  exit /b 1
)

call gradlew :app:verify16KbAll
if errorlevel 1 (
  popd
  exit /b 1
)
popd

echo SUCCESS: 16KB-compatible GDAL drop-in build and validation completed.
exit /b 0

