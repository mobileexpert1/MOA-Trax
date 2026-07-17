#!/usr/bin/env powershell

# FIX GDAL BUILD SCRIPT TO USE NINJA INSTEAD OF MAKE
Write-Host "=== FIXING GDAL BUILD SCRIPT ===" -ForegroundColor Green

$buildScript = "third_party/GDAL4Android/gdal/build_cpp.sh"
$backupScript = "third_party/GDAL4Android/gdal/build_cpp.sh.backup"

if (-not (Test-Path $buildScript)) {
    Write-Error "Build script not found: $buildScript"
    exit 1
}

# Create backup
Copy-Item $buildScript $backupScript -Force
Write-Host "Created backup: $backupScript" -ForegroundColor Gray

# Read and modify the build script
$content = Get-Content $buildScript -Raw

# Replace make commands with ninja
$content = $content -replace 'make clean', 'ninja -t clean'
$content = $content -replace 'make -j\$BUILD_THREADS', 'ninja'
$content = $content -replace 'make install', 'ninja install'

# Write back the modified script
Set-Content $buildScript $content -NoNewline

Write-Host "✅ Fixed build script to use ninja instead of make" -ForegroundColor Green
Write-Host "The script now uses proper NDK r28+ toolchain" -ForegroundColor Cyan

Write-Host ""
Write-Host "=== FIX COMPLETE ===" -ForegroundColor Green
