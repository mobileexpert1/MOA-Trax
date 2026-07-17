#!/usr/bin/env powershell

# DIRECT GDAL REBUILD - Bypass Gradle, use shell directly
Write-Host "=== DIRECT GDAL REBUILD WITH 16KB FLAGS ===" -ForegroundColor Green

$gdalDir = "third_party/GDAL4Android/gdal"
$bashPath = "C:\Program Files\Git\bin\bash.exe"
$buildScript = "build_cpp.sh"
$ndkPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358"
$minSdk = "21"
$javaHome = "C:\Program Files\Android\Android Studio\jbr"
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
    Write-Host "Starting direct GDAL rebuild with 16KB alignment flags..." -ForegroundColor Cyan
    Write-Host "NDK Path: $ndkPath" -ForegroundColor Gray
    Write-Host "Build Type: $buildType" -ForegroundColor Gray
    
    # Change to GDAL directory
    Push-Location $gdalDir
    
    # First, clean any existing build artifacts
    Write-Host "Cleaning existing build artifacts..." -ForegroundColor Yellow
    if (Test-Path "cpp") {
        Remove-Item -Recurse -Force "cpp" -ErrorAction SilentlyContinue
    }
    if (Test-Path "libs") {
        Remove-Item -Recurse -Force "libs" -ErrorAction SilentlyContinue
    }
    
    # Run the build script directly with bash
    Write-Host "Running: bash $buildScript '$ndkPath' '$minSdk' '$javaHome' '$buildType'" -ForegroundColor Yellow
    
    # Execute the build directly
    $currentDir = $pwd.Path.Replace('\','/')
    $output = & $bashPath -c "cd '$currentDir' && ./$buildScript '$ndkPath' '$minSdk' '$javaHome' '$buildType'" 2>&1
    $exitCode = $LASTEXITCODE
    
    Write-Host "Build output (last 50 lines):" -ForegroundColor Gray
    $outputLines = $output -split "`n"
    $outputLines | Select-Object -Last 50 | ForEach-Object { Write-Host $_ }
    
    if ($exitCode -eq 0) {
        Write-Host "✅ GDAL rebuild completed successfully!" -ForegroundColor Green
        
        # Check if new libraries were created
        $libsDir = "libs"
        if (Test-Path $libsDir) {
            Write-Host "New libraries created:" -ForegroundColor Cyan
            Get-ChildItem -Path $libsDir -Recurse -Filter "*.aar" | ForEach-Object {
                Write-Host "  $($_.FullName)" -ForegroundColor White
            }
            
            # Copy the new AAR to the app
            $newAar = "libs/gdal-debug.aar"
            if (Test-Path $newAar) {
                $targetAar = "../../app/src/main/libs/gdal-debug-16kb.aar"
                Copy-Item $newAar $targetAar -Force
                Write-Host "✅ Copied new 16KB-aligned AAR to: $targetAar" -ForegroundColor Green
                
                # Also copy the JAR for class resolution
                if (Test-Path "../../app/src/main/libs/gdal-classes.jar") {
                    Write-Host "✅ GDAL classes JAR already exists" -ForegroundColor Green
                } else {
                    Write-Host "⚠️  Extracting GDAL classes JAR..." -ForegroundColor Yellow
                    & ../../scripts/extract-gdal-jar.ps1
                }
            }
        }
        
    } else {
        Write-Error "❌ GDAL rebuild failed with exit code: $exitCode"
        Write-Host "Check the full output above for error details" -ForegroundColor Red
        exit 1
    }
    
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== REBUILD COMPLETE ===" -ForegroundColor Green
