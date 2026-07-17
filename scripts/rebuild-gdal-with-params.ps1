#!/usr/bin/env powershell

# REBUILD GDAL WITH CORRECT PARAMETERS AND 16KB FLAGS
Write-Host "=== REBUILDING GDAL WITH 16KB FLAGS ===" -ForegroundColor Green

$gdalDir = "third_party/GDAL4Android/gdal"
$bashPath = "C:\Program Files\Git\bin\bash.exe"
$buildScript = "build_cpp.sh"
$ndkPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358"
$minSdk = "21"
$javaHome = "C:\Program Files\Android\Android Studio\jbr\bin\..\"
$buildType = "Debug"

if (-not (Test-Path $bashPath)) {
    Write-Error "Git Bash not found at: $bashPath"
    exit 1
}

if (-not (Test-Path "$gdalDir/$buildScript")) {
    Write-Error "GDAL build script not found: $gdalDir/$buildScript"
    exit 1
}

if (-not (Test-Path $ndkPath)) {
    Write-Error "NDK not found at: $ndkPath"
    exit 1
}

try {
    Write-Host "Starting GDAL rebuild with 16KB alignment flags..." -ForegroundColor Cyan
    Write-Host "NDK Path: $ndkPath" -ForegroundColor Gray
    Write-Host "Min SDK: $minSdk" -ForegroundColor Gray
    Write-Host "Build Type: $buildType" -ForegroundColor Gray
    
    # Change to GDAL directory and run build script with parameters
    Push-Location $gdalDir
    
    Write-Host "Running: bash $buildScript '$ndkPath' '$minSdk' '$javaHome' '$buildType'" -ForegroundColor Yellow
    $output = & $bashPath $buildScript $ndkPath $minSdk $javaHome $buildType 2>&1
    
    Write-Host "Build output:" -ForegroundColor Gray
    Write-Host $output
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ GDAL rebuild completed successfully!" -ForegroundColor Green
        
        # Check if new libraries were created
        $libsDir = "libs"
        if (Test-Path $libsDir) {
            Write-Host "New libraries created:" -ForegroundColor Cyan
            Get-ChildItem -Path $libsDir -Recurse -Filter "*.aar" | ForEach-Object {
                Write-Host "  $($_.FullName)" -ForegroundColor White
            }
        }
        
        # Verify the new AAR has 16KB alignment
        $newAar = "libs/gdal-debug.aar"
        if (Test-Path $newAar) {
            Write-Host "`nVerifying new AAR alignment..." -ForegroundColor Cyan
            # Copy to 16KB location
            Copy-Item $newAar "../../app/src/main/libs/gdal-debug-16kb.aar" -Force
            Write-Host "Copied new AAR to app/libs as gdal-debug-16kb.aar" -ForegroundColor Green
        }
        
    } else {
        Write-Error "❌ GDAL rebuild failed with exit code: $LASTEXITCODE"
        Write-Host "Error details: $output" -ForegroundColor Red
        exit 1
    }
    
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== REBUILD COMPLETE ===" -ForegroundColor Green
