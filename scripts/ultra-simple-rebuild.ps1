#!/usr/bin/env powershell

# ULTRA SIMPLE REBUILD - No Docker, Minimal Setup
Write-Host "=== ULTRA SIMPLE 16KB REBUILD ===" -ForegroundColor Green

Write-Host "This is the SIMPLEST rebuild method:" -ForegroundColor Cyan
Write-Host "✅ Uses only existing tools" -ForegroundColor White
Write-Host "✅ No Docker required" -ForegroundColor White
Write-Host "✅ No complex setup" -ForegroundColor White
Write-Host "✅ Step-by-step commands" -ForegroundColor White

# Step 1: Find and use existing cmake
Write-Host "Step 1: Finding cmake..." -ForegroundColor Yellow

$cmakePath = $null
$possiblePaths = @(
    "C:\Program Files\Android\Android Studio\cmake\bin\cmake.exe",
    "C:\Program Files\Android\Android Studio\jbr\bin\cmake.exe",
    "C:\Program Files\Android\Android Studio\bin\cmake.exe"
)

foreach ($path in $possiblePaths) {
    if (Test-Path $path) {
        $cmakePath = $path
        Write-Host "✅ Found cmake: $cmakePath" -ForegroundColor Green
        break
    }
}

if (-not $cmakePath) {
    # Search in Android Studio directory
    $cmakeSearch = Get-ChildItem -Path "C:\Program Files\Android\Android Studio" -Recurse -Filter "cmake.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($cmakeSearch) {
        $cmakePath = $cmakeSearch.FullName
        Write-Host "✅ Found cmake: $cmakePath" -ForegroundColor Green
    } else {
        Write-Host "❌ cmake not found. Using system cmake..." -ForegroundColor Red
        $cmakePath = "cmake"
    }
}

# Step 2: Create ultra-simple build script
Write-Host "Step 2: Creating ultra-simple build..." -ForegroundColor Yellow

$gdalDir = "third_party/GDAL4Android/gdal"
$ndkPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358"

$buildScript = @"
#!/bin/bash
# Ultra Simple GDAL rebuild with 16KB flags

echo "=== ULTRA SIMPLE 16KB REBUILD ==="

# Set 16KB alignment flags
export CFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
export CXXFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
export LDFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"

echo "✅ 16KB flags set"
echo "CFLAGS: \$CFLAGS"

# Use existing build script but with fixed paths
cd "$gdalDir"

# Clean previous build
if [ -d "cpp" ]; then
    rm -rf cpp
fi
if [ -d "libs" ]; then
    rm -rf libs
fi

echo "✅ Previous build cleaned"

# Run the existing build script with correct parameters
echo "✅ Starting GDAL rebuild..."
echo "NDK: $ndkPath"
echo "Min SDK: 21"
echo "Java Home: C:\Program Files\Android\Android Studio\jbr"
echo "Build Type: Debug"

# Execute build
./build_cpp.sh "$ndkPath" "21" "C:\Program Files\Android\Android Studio\jbr" "Debug"

echo "=== REBUILD COMPLETE ==="
"@

$scriptPath = Join-Path $env:TEMP "ultra_simple_build.sh"
Set-Content $scriptPath $buildScript

Write-Host "✅ Ultra-simple build script created" -ForegroundColor Green

# Step 3: Execute the rebuild
Write-Host "Step 3: Executing rebuild..." -ForegroundColor Yellow

try {
    Push-Location $gdalDir
    
    # Make script executable
    $bashPath = "C:\Program Files\Git\bin\bash.exe"
    
    Write-Host "Running ultra-simple rebuild..." -ForegroundColor Cyan
    Write-Host "This may take 10-20 minutes..." -ForegroundColor Gray
    
    # Execute with timeout handling
    $job = Start-Job -ScriptBlock {
        param($bash, $script)
        & $bash $script 2>&1
    } -ArgumentList $bashPath, $scriptPath
    
    # Wait for completion with progress
    $timeout = 1800 # 30 minutes
    $elapsed = 0
    
    while ($job.State -eq 'Running' -and $elapsed -lt $timeout) {
        Start-Sleep 30
        $elapsed += 30
        Write-Host "Building... ($($elapsed/60) minutes elapsed)" -ForegroundColor Gray
    }
    
    if ($job.State -eq 'Running') {
        Write-Host "⚠️  Build taking too long, stopping..." -ForegroundColor Yellow
        Stop-Job $job
        Remove-Job $job
        Write-Host "👉 Try the manual build below" -ForegroundColor Yellow
    } else {
        $output = Receive-Job $job
        Remove-Job $job
        
        Write-Host "Build output:" -ForegroundColor Gray
        Write-Host $output
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Ultra-simple rebuild completed!" -ForegroundColor Green
            
            # Copy new AAR
            if (Test-Path "libs/gdal-debug.aar") {
                Copy-Item "libs/gdal-debug.aar" "../../app/src/main/libs/gdal-debug-16kb.aar" -Force
                Write-Host "✅ New 16KB AAR copied to app/libs" -ForegroundColor Green
            }
        } else {
            Write-Host "⚠️  Build had issues. See output above." -ForegroundColor Yellow
        }
    }
    
} finally {
    Pop-Location
}

# Step 4: Manual fallback if needed
Write-Host ""
Write-Host "=== MANUAL BUILD INSTRUCTIONS ===" -ForegroundColor Yellow
Write-Host "If automated build fails, run these commands manually:" -ForegroundColor Gray
Write-Host ""
Write-Host "1. Open Git Bash" -ForegroundColor White
Write-Host "2. cd to: $gdalDir" -ForegroundColor White
Write-Host "3. Run: ./build_cpp.sh '$ndkPath' 21 'C:\Program Files\Android\Android Studio\jbr' Debug" -ForegroundColor White
Write-Host ""
Write-Host "This will rebuild GDAL with 16KB alignment flags." -ForegroundColor Cyan

Write-Host ""
Write-Host "=== ULTRA SIMPLE REBUILD COMPLETE ===" -ForegroundColor Green
