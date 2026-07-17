#!/usr/bin/env powershell

# Extract GDAL JAR from AAR for direct classpath access
Write-Host "=== Extracting GDAL JAR ===" -ForegroundColor Green

$aarPath = "app/src/main/libs/gdal-debug-16kb.aar"
$jarPath = "app/src/main/libs/gdal-classes.jar"
$tempDir = Join-Path $env:TEMP "extract_gdal_$(Get-Random)"

if (-not (Test-Path $aarPath)) {
    Write-Error "16KB GDAL AAR not found: $aarPath"
    exit 1
}

New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

try {
    Write-Host "Extracting AAR..." -ForegroundColor Cyan
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($aarPath, $tempDir)
    
    $libsJar = Join-Path $tempDir "libs\gdal.jar"
    if (Test-Path $libsJar) {
        Write-Host "Found GDAL JAR, copying to libs directory..." -ForegroundColor Cyan
        Copy-Item $libsJar $jarPath -Force
        
        $fileInfo = Get-Item $jarPath
        Write-Host "SUCCESS: GDAL JAR extracted!" -ForegroundColor Green
        Write-Host "Location: $jarPath" -ForegroundColor Yellow
        Write-Host "Size: $($fileInfo.Length) bytes" -ForegroundColor Gray
    } else {
        Write-Error "GDAL JAR not found in AAR"
        exit 1
    }
    
} finally {
    if (Test-Path $tempDir) {
        Remove-Item -Path $tempDir -Recurse -Force
    }
}

Write-Host ""
Write-Host "=== EXTRACTION COMPLETE ===" -ForegroundColor Green
Write-Host "The GDAL JAR is now available for direct classpath access." -ForegroundColor Yellow
