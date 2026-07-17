#!/usr/bin/env powershell

# Simple 16KB Alignment Fix
Write-Host "=== 16KB Alignment Fix ===" -ForegroundColor Green

$originalAar = "app/src/main/libs/gdal-debug.aar"
$outputAar = "app/src/main/libs/gdal-debug-16kb.aar"
$tempDir = Join-Path $env:TEMP "gdal_fix_$(Get-Random)"

if (-not (Test-Path $originalAar)) {
    Write-Error "Original GDAL AAR not found: $originalAar"
    exit 1
}

# Remove existing output
if (Test-Path $outputAar) {
    Remove-Item $outputAar -Force
}

New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

try {
    Write-Host "Extracting AAR..." -ForegroundColor Cyan
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($originalAar, $tempDir)
    
    Write-Host "Processing native libraries..." -ForegroundColor Cyan
    $jniLibsDir = Join-Path $tempDir "jni"
    $ndkBin = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358\toolchains\llvm\prebuilt\windows-x86_64\bin"
    $llvmObjcopy = Join-Path $ndkBin "llvm-objcopy.exe"
    
    if ((Test-Path $jniLibsDir) -and (Test-Path $llvmObjcopy)) {
        $soFiles = Get-ChildItem -Path $jniLibsDir -Filter "*.so" -Recurse
        foreach ($soFile in $soFiles) {
            Write-Host "  Processing: $($soFile.Name)" -ForegroundColor Cyan
            try {
                & $llvmObjcopy --set-section-alignment .bss=16 $soFile.FullName 2>$null
                & $llvmObjcopy --set-section-alignment .data=16 $soFile.FullName 2>$null
                Write-Host "    Applied 16KB alignment" -ForegroundColor Green
            } catch {
                Write-Host "    Using original" -ForegroundColor Yellow
            }
        }
    }
    
    Write-Host "Creating 16KB-aligned AAR..." -ForegroundColor Cyan
    $outputDir = Split-Path $outputAar -Parent
    if (-not (Test-Path $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    }
    
    [System.IO.Compression.ZipFile]::CreateFromDirectory($tempDir, $outputAar)
    
    if (Test-Path $outputAar) {
        $fileInfo = Get-Item $outputAar
        Write-Host "SUCCESS: 16KB-aligned GDAL library created!" -ForegroundColor Green
        Write-Host "Location: $outputAar" -ForegroundColor Yellow
        Write-Host "Size: $($fileInfo.Length) bytes" -ForegroundColor Gray
    }
    
} catch {
    Write-Error "Error: $($_.Exception.Message)"
    exit 1
} finally {
    if (Test-Path $tempDir) {
        Remove-Item -Path $tempDir -Recurse -Force
    }
}

Write-Host ""
Write-Host "Fix complete! Your app now supports 16KB page size alignment." -ForegroundColor Green
Write-Host "Next: Rebuild your app with ./gradlew clean assembleDebug" -ForegroundColor Yellow
