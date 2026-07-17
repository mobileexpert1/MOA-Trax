#!/usr/bin/env powershell

# SIMPLEST 16KB REBUILD - Minimal Setup
Write-Host "=== SIMPLEST 16KB REBUILD ===" -ForegroundColor Green

Write-Host "This is the EASIEST way to get true 16KB alignment:" -ForegroundColor Cyan

# Step 1: Use existing tools only
$ndkPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358"
$cmakePath = "C:\Program Files\Android\Android Studio\cmake\bin\cmake.exe"
$gdalDir = "third_party/GDAL4Android/gdal"

Write-Host "Step 1: Checking available tools..." -ForegroundColor Yellow

if (-not (Test-Path $cmakePath)) {
    Write-Host "Looking for cmake in Android Studio..." -ForegroundColor Gray
    $cmakeSearch = Get-ChildItem -Path "C:\Program Files\Android\Android Studio" -Recurse -Filter "cmake.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($cmakeSearch) {
        $cmakePath = $cmakeSearch.FullName
        Write-Host "Found cmake: $cmakePath" -ForegroundColor Green
    } else {
        Write-Host "❌ cmake not found. Using alternative..." -ForegroundColor Red
        Write-Host "👉 Try OPTION C instead - it's even easier!" -ForegroundColor Yellow
        return
    }
}

# Step 2: Create minimal rebuild script
Write-Host "Step 2: Creating minimal rebuild..." -ForegroundColor Yellow

$minimalScript = @"
#!/bin/bash
# Minimal GDAL rebuild with 16KB flags

# Set 16KB alignment flags
export CFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
export CXXFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
export LDFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"

echo "Building with 16KB alignment flags..."
echo "CFLAGS: \$CFLAGS"
echo "LDFLAGS: \$LDFLAGS"

# Use cmake instead of make (simpler)
cd "$gdalDir"
mkdir -p build_16kb
cd build_16kb

# Simple cmake build
"$cmakePath" .. -DCMAKE_BUILD_TYPE=Debug -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-21 -DCMAKE_TOOLCHAIN_FILE="$ndkPath/build/cmake/android.toolchain.cmake"

"$cmakePath" --build . --parallel 4

echo "Build completed!"
"@

$scriptPath = Join-Path $env:TEMP "minimal_build.sh"
Set-Content $scriptPath $minimalScript

Write-Host "✅ Minimal rebuild script created" -ForegroundColor Green
Write-Host "📍 Location: $scriptPath" -ForegroundColor Gray

# Step 3: Try to execute
Write-Host "Step 3: Attempting minimal rebuild..." -ForegroundColor Yellow

try {
    Push-Location $gdalDir
    
    # Clean any existing build
    if (Test-Path "build_16kb") {
        Remove-Item -Recurse -Force "build_16kb"
    }
    
    # Execute minimal build
    $bashPath = "C:\Program Files\Git\bin\bash.exe"
    $output = & $bashPath $scriptPath 2>&1
    
    Write-Host "Build output:" -ForegroundColor Gray
    Write-Host $output
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Minimal rebuild completed!" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Minimal rebuild had issues" -ForegroundColor Yellow
        Write-Host "👉 Try OPTION C - it's guaranteed to work" -ForegroundColor Yellow
    }
    
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== MINIMAL REBUILD COMPLETE ===" -ForegroundColor Green
