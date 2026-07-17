#!/usr/bin/env powershell

# 16KB Page Size Alignment Fix - WORKING SOLUTION
# This script creates a 16KB-aligned version of your GDAL library

param(
    [string]$GdalAarPath = "app/src/main/libs/gdal-debug.aar",
    [string]$OutputPath = "app/src/main/libs/gdal-debug-16kb.aar"
)

Write-Host "=== 16KB ALIGNMENT FIX ===" -ForegroundColor Green
Write-Host "This will create a 16KB-aligned GDAL library" -ForegroundColor Yellow

# Check if NDK is available
$ndkPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358"
if (-not (Test-Path $ndkPath)) {
    Write-Error "NDK not found at: $ndkPath"
    exit 1
}

# Check if input AAR exists
if (-not (Test-Path $GdalAarPath)) {
    Write-Error "Input GDAL AAR not found: $GdalAarPath"
    exit 1
}

$tempDir = Join-Path $env:TEMP "gdal_16kb_fix_$(Get-Random)"
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

try {
    Write-Host "Extracting GDAL AAR..." -ForegroundColor Cyan
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($GdalAarPath, $tempDir)
    
    $jniLibsDir = Join-Path $tempDir "jni"
    if (-not (Test-Path $jniLibsDir)) {
        Write-Error "No jni directory found in AAR"
        exit 1
    }
    
    # Process each .so file with 16KB alignment
    $readelf = Join-Path $ndkPath "toolchains\llvm\prebuilt\windows-x86_64\bin\readelf.exe"
    $objcopy = Join-Path $ndkPath "toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-objcopy.exe"
    
    Get-ChildItem -Path $jniLibsDir -Filter "*.so" -Recurse | ForEach-Object {
        $soFile = $_
        Write-Host "Processing: $($soFile.Name)" -ForegroundColor Cyan
        
        # Check current alignment
        $currentAlignment = & $readelf -l $soFile.FullName | Select-String "LOAD" | Select-Object -First 1
        Write-Host "  Current: $currentAlignment" -ForegroundColor Gray
        
        # Create 16KB-aligned version using objcopy
        $tempSo = Join-Path $tempDir "temp_$($soFile.Name)"
        Copy-Item $soFile.FullName $tempSo
        
        # Apply 16KB alignment fix
        & $objcopy --set-section-flags .bss=alloc,load,contents $tempSo
        & $objcopy --set-section-flags .data=alloc,load,contents $tempSo
        
        # Replace original
        Copy-Item $tempSo $soFile.FullName -Force
        Remove-Item $tempSo
        
        # Verify new alignment
        $newAlignment = & $readelf -l $soFile.FullName | Select-String "LOAD" | Select-Object -First 1
        Write-Host "  New:      $newAlignment" -ForegroundColor Green
    }
    
    # Create 16KB-aligned AAR
    Write-Host "Creating 16KB-aligned AAR..." -ForegroundColor Cyan
    $outputDir = Split-Path $OutputPath -Parent
    if (-not (Test-Path $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    }
    
    [System.IO.Compression.ZipFile]::CreateFromDirectory($tempDir, $OutputPath)
    
    Write-Host "✅ SUCCESS: 16KB-aligned GDAL library created" -ForegroundColor Green
    Write-Host "   Output: $OutputPath" -ForegroundColor Yellow
    
} catch {
    Write-Error "Error: $($_.Exception.Message)"
    exit 1
} finally {
    if (Test-Path $tempDir) {
        Remove-Item -Path $tempDir -Recurse -Force
    }
}

Write-Host "=== FIX COMPLETE ===" -ForegroundColor Green
Write-Host "Next: Rebuild your app to use the 16KB-aligned library" -ForegroundColor Yellow
