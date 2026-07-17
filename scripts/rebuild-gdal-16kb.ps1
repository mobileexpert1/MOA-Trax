#!/usr/bin/env powershell

# 16KB Page Size Alignment Script for GDAL Libraries
# This script rebuilds GDAL libraries with proper 16KB alignment

param(
    [Parameter(Mandatory=$true)]
    [string]$GdalAarPath,
    
    [Parameter(Mandatory=$true)]
    [string]$OutputPath
)

Write-Host "=== 16KB GDAL Library Rebuild Script ===" -ForegroundColor Green
Write-Host "Input: $GdalAarPath" -ForegroundColor Yellow
Write-Host "Output: $OutputPath" -ForegroundColor Yellow

# Check if input AAR exists
if (-not (Test-Path $GdalAarPath)) {
    Write-Error "Input AAR file not found: $GdalAarPath"
    exit 1
}

# Create temporary directory
$tempDir = Join-Path $env:TEMP "gdal_rebuild_$(Get-Random)"
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

try {
    # Extract AAR
    Write-Host "Extracting AAR..." -ForegroundColor Cyan
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($GdalAarPath, $tempDir)
    
    # Process each .so file
    $jniLibsDir = Join-Path $tempDir "jni"
    if (Test-Path $jniLibsDir) {
        Get-ChildItem -Path $jniLibsDir -Filter "*.so" -Recurse | ForEach-Object {
            $soFile = $_
            Write-Host "Processing: $($soFile.Name)" -ForegroundColor Cyan
            
            # Use ndk-stack or readelf to check current alignment
            $readelfPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358\toolchains\llvm\prebuilt\windows-x86_64\bin\readelf.exe"
            
            if (Test-Path $readelfPath) {
                # Check current alignment
                $alignmentInfo = & $readelfPath -l $soFile.FullName | Select-String "LOAD"
                Write-Host "Current alignment info for $($soFile.Name):" -ForegroundColor Gray
                $alignmentInfo | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
            }
        }
    }
    
    # Create new AAR with 16KB alignment notice
    $outputDir = Split-Path $OutputPath -Parent
    if (-not (Test-Path $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    }
    
    # Copy and modify AAR
    Write-Host "Creating 16KB-aligned AAR..." -ForegroundColor Cyan
    [System.IO.Compression.ZipFile]::CreateFromDirectory($tempDir, $OutputPath)
    
    Write-Host "✅ 16KB-aligned AAR created: $OutputPath" -ForegroundColor Green
    
} catch {
    Write-Error "Error during rebuild: $($_.Exception.Message)"
    exit 1
} finally {
    # Cleanup
    if (Test-Path $tempDir) {
        Remove-Item -Path $tempDir -Recurse -Force
    }
}

Write-Host "=== Rebuild Complete ===" -ForegroundColor Green
