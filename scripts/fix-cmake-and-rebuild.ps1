#!/usr/bin/env powershell

# FIX CMAKE AND REBUILD - Complete solution
Write-Host "=== FIX CMAKE AND REBUILD ===" -ForegroundColor Green

Write-Host "STEP 1: Installing cmake..." -ForegroundColor Yellow

# Download cmake manually
$cmakeUrl = "https://github.com/Kitware/CMake/releases/download/v3.28.1/cmake-3.28.1-windows-x86_64.zip"
$cmakeZip = Join-Path $env:TEMP "cmake.zip"
$cmakeDir = "C:\cmake"

if (-not (Test-Path $cmakeDir)) {
    Write-Host "Downloading cmake..." -ForegroundColor Cyan
    try {
        Invoke-WebRequest -Uri $cmakeUrl -OutFile $cmakeZip -UseBasicParsing
        Write-Host "Extracting cmake..." -ForegroundColor Cyan
        Expand-Archive -Path $cmakeZip -DestinationPath "C:\" -Force
        Rename-Item "C:\cmake-3.28.1-windows-x86_64" $cmakeDir
        Remove-Item $cmakeZip
        Write-Host "✅ cmake installed to: $cmakeDir" -ForegroundColor Green
    } catch {
        Write-Host "❌ Failed to install cmake" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✅ cmake already installed" -ForegroundColor Green
}

$cmakePath = "$cmakeDir\bin\cmake.exe"
if (Test-Path $cmakePath) {
    Write-Host "✅ cmake found: $cmakePath" -ForegroundColor Green
} else {
    Write-Host "❌ cmake not found" -ForegroundColor Red
    exit 1
}

# Add cmake to PATH for this session
$env:PATH = "$cmakeDir\bin;$env:PATH"

Write-Host ""
Write-Host "STEP 2: Fixing build script..." -ForegroundColor Yellow

$gdalDir = "third_party/GDAL4Android/gdal"
$originalScript = "$gdalDir/build_cpp.sh"
$fixedScript = "$gdalDir/build_cpp_fixed.sh"

# Read original script and fix it properly
$scriptContent = Get-Content $originalScript -Raw

# Fix the wget lines by commenting them out
$scriptContent = $scriptContent -replace '^(.*wget.*tar\.gz.*)', '# $1'

# Ensure 16KB flags are present
if ($scriptContent -match "export  CFLAGS=`"([^`"]*)`"") {
    $currentCflags = $matches[1]
    if ($currentCflags -notmatch "max-page-size=16384") {
        $newCflags = "$currentCflags -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
        $scriptContent = $scriptContent -replace "export  CFLAGS=`"$currentCflags`"", "export  CFLAGS=`"$newCflags`""
    }
}

if ($scriptContent -match "export  CXXFLAGS=`"([^`"]*)`"") {
    $currentCxxflags = $matches[1]
    if ($currentCxxflags -notmatch "max-page-size=16384") {
        $newCxxflags = "$currentCxxflags -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
        $scriptContent = $scriptContent -replace "export  CXXFLAGS=`"$currentCxxflags`"", "export  CXXFLAGS=`"$newCxxflags`""
    }
}

if ($scriptContent -match "export  LDFLAGS=`"([^`"]*)`"") {
    $currentLdflags = $matches[1]
    if ($currentLdflags -notmatch "max-page-size=16384") {
        $newLdflags = "$currentLdflags -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
        $scriptContent = $scriptContent -replace "export  LDFLAGS=`"$currentLdflags`"", "export  LDFLAGS=`"$newLdflags`""
    }
}

# Fix cmake path
$scriptContent = $scriptContent -replace 'CMAKE_BIN=`$(which cmake)', "CMAKE_BIN=`"$cmakePath`""

Set-Content $fixedScript $scriptContent -NoNewline
Write-Host "✅ Fixed build script created" -ForegroundColor Green

Write-Host ""
Write-Host "STEP 3: Running 16KB rebuild..." -ForegroundColor Yellow

try {
    Push-Location $gdalDir
    
    # Clean previous build
    if (Test-Path "cpp") {
        Remove-Item -Recurse -Force "cpp"
    }
    if (Test-Path "libs") {
        Remove-Item -Recurse -Force "libs"
    }
    
    # Copy extracted dependencies
    $depsDir = "$gdalDir/dependencies"
    if (Test-Path "$depsDir") {
        Copy-Item -Recurse -Force "$depsDir\*" "cpp\"
    }
    
    $bashPath = "C:\Program Files\Git\bin\bash.exe"
    $ndkPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358"
    
    Write-Host "Running 16KB rebuild with cmake..." -ForegroundColor Cyan
    
    # Set environment variables for bash
    $env:CCMAKE_BIN = $cmakePath
    
    # Run the fixed build script
    $output = & $bashPath -c "./build_cpp_fixed.sh '$ndkPath' 21 'C:\Program Files\Android\Android Studio\jbr' Debug" 2>&1
    
    Write-Host "Build output (last 50 lines):" -ForegroundColor Gray
    $outputLines = $output -split "`n"
    $outputLines | Select-Object -Last 50 | ForEach-Object { Write-Host $_ }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 16KB rebuild completed successfully!" -ForegroundColor Green
        
        # Check if new AAR was created
        if (Test-Path "libs/gdal-debug.aar") {
            Write-Host "✅ New AAR created" -ForegroundColor Green
            
            # Copy to app directory
            $targetAar = "../../app/src/main/libs/gdal-debug-16kb.aar"
            Copy-Item "libs/gdal-debug.aar" $targetAar -Force
            Write-Host "✅ AAR copied to: $targetAar" -ForegroundColor Green
            
            # Verify 16KB alignment
            Write-Host ""
            Write-Host "STEP 4: Verifying 16KB alignment..." -ForegroundColor Yellow
            & ../../scripts/check-load-segments.ps1
        } else {
            Write-Host "❌ AAR not created" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ 16KB rebuild failed" -ForegroundColor Red
        Write-Host "Exit code: $LASTEXITCODE" -ForegroundColor Gray
    }
    
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== REBUILD COMPLETE ===" -ForegroundColor Green
