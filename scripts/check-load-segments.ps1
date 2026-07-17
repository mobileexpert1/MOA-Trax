#!/usr/bin/env powershell

# CHECK LOAD SEGMENT ALIGNMENT WITH READELF
Write-Host "=== CHECKING LOAD SEGMENT ALIGNMENT ===" -ForegroundColor Green

$ndkBin = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358\toolchains\llvm\prebuilt\windows-x86_64\bin"
$readelf = Join-Path $ndkBin "llvm-readelf.exe"

# Check 16KB AAR
$aarPath = "app/src/main/libs/gdal-debug-16kb.aar"
if (-not (Test-Path $aarPath)) {
    Write-Error "16KB AAR not found: $aarPath"
    exit 1
}

$tempDir = Join-Path $env:TEMP "check_load_$(Get-Random)"
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

try {
    Write-Host "Extracting AAR..." -ForegroundColor Cyan
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($aarPath, $tempDir)
    
    $jniLibsDir = Join-Path $tempDir "jni"
    $arm64Dir = Join-Path $jniLibsDir "arm64-v8a"
    
    if (Test-Path $arm64Dir) {
        Write-Host "`n=== ARM64-v8a Libraries ===" -ForegroundColor Yellow
        
        $targetLibs = @("libgdal.so", "libgdalalljni.so", "libgdalwrap.so", "libproj.so", "libc++_shared.so")
        
        foreach ($libName in $targetLibs) {
            $libPath = Join-Path $arm64Dir $libName
            if (Test-Path $libPath) {
                Write-Host "`n${libName}:" -ForegroundColor Cyan
                
                # Get readelf output
                $output = & $readelf -l $libPath 2>&1
                $loadLines = $output | Where-Object { $_ -match "LOAD" }
                
                if ($loadLines) {
                    foreach ($line in $loadLines) {
                        Write-Host "  $line" -ForegroundColor White
                        
                        # Extract alignment value
                        if ($line -match "Align\s+(0x[0-9a-fA-F]+)") {
                            $align = $matches[1]
                            $alignValue = [Convert]::ToInt32($align, 16)
                            
                            if ($alignValue -eq 16384) {
                                Write-Host "    ✅ $align = 16KB" -ForegroundColor Green
                            } elseif ($alignValue -eq 4096) {
                                Write-Host "    ❌ $align = 4KB (WRONG!)" -ForegroundColor Red
                            } else {
                                Write-Host "    ⚠️ $align = $($alignValue) bytes" -ForegroundColor Yellow
                            }
                        }
                    }
                } else {
                    Write-Host "  No LOAD segments found!" -ForegroundColor Red
                }
            } else {
                Write-Host "`n${libName}: NOT FOUND" -ForegroundColor Red
            }
        }
    } else {
        Write-Error "ARM64-v8a directory not found"
    }
    
} finally {
    if (Test-Path $tempDir) {
        Remove-Item -Path $tempDir -Recurse -Force
    }
}

Write-Host "`n=== VERIFICATION COMPLETE ===" -ForegroundColor Green
