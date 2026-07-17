#!/usr/bin/env powershell

# VERIFY ACTUAL BINARY ALIGNMENT WITH READELF
Write-Host "=== VERIFYING BINARY ALIGNMENT ===" -ForegroundColor Green

$ndkBin = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358\toolchains\llvm\prebuilt\windows-x86_64\bin"
$readelf = Join-Path $ndkBin "llvm-readelf.exe"

if (-not (Test-Path $readelf)) {
    Write-Error "readelf not found at: $readelf"
    exit 1
}

# Check both original and 16KB AAR
$aarPaths = @(
    "app/src/main/libs/gdal-debug.aar",
    "app/src/main/libs/gdal-debug-16kb.aar"
)

foreach ($aarPath in $aarPaths) {
    if (Test-Path $aarPath) {
        Write-Host "`n=== Checking: $(Split-Path $aarPath -Leaf) ===" -ForegroundColor Yellow
        
        $tempDir = Join-Path $env:TEMP "verify_$(Split-Path $aarPath -Leaf)_$(Get-Random)"
        New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
        
        try {
            Add-Type -AssemblyName System.IO.Compression.FileSystem
            [System.IO.Compression.ZipFile]::ExtractToDirectory($aarPath, $tempDir)
            
            $jniLibsDir = Join-Path $tempDir "jni"
            if (Test-Path $jniLibsDir) {
                $soFiles = Get-ChildItem -Path $jniLibsDir -Filter "*.so" -Recurse | Sort-Object Name
                
                foreach ($soFile in $soFiles) {
                    Write-Host "`n  $($soFile.Name):" -ForegroundColor Cyan
                    
                    # Use readelf to check LOAD segments
                    $output = & $readelf -l $soFile.FullName 2>$1
                    $loadLines = $output | Where-Object { $_ -match "LOAD" }
                    
                    foreach ($line in $loadLines) {
                        if ($line -match "Align\s+(0x[0-9a-fA-F]+)") {
                            $align = $matches[1]
                            $alignValue = [Convert]::ToInt32($align, 16)
                            
                            if ($alignValue -eq 16384) {
                                Write-Host "    LOAD: $align (16KB) ✅" -ForegroundColor Green
                            } elseif ($alignValue -eq 4096) {
                                Write-Host "    LOAD: $align (4KB) ❌" -ForegroundColor Red
                            } else {
                                Write-Host "    LOAD: $align ($($alignValue) bytes) ⚠️" -ForegroundColor Yellow
                            }
                        }
                    }
                }
            }
        } finally {
            if (Test-Path $tempDir) {
                Remove-Item -Path $tempDir -Recurse -Force
            }
        }
    }
}

Write-Host "`n=== VERIFICATION COMPLETE ===" -ForegroundColor Green
