#!/usr/bin/env powershell

# DIAGNOSE BUILD FAILURE - Identify exact issue
Write-Host "=== DIAGNOSING BUILD FAILURE ===" -ForegroundColor Green

$gdalDir = "third_party/GDAL4Android/gdal"
$buildScript = "$gdalDir/build_cpp.sh"
$bashPath = "C:\Program Files\Git\bin\bash.exe"

Write-Host "STEP 1: Checking build script..." -ForegroundColor Yellow

if (-not (Test-Path $buildScript)) {
    Write-Host "❌ Build script not found!" -ForegroundColor Red
    exit 1
}

# Check if 16KB flags are present
$scriptContent = Get-Content $buildScript -Raw
if ($scriptContent -match "max-page-size=16384") {
    Write-Host "✅ 16KB flags found in build script" -ForegroundColor Green
} else {
    Write-Host "❌ 16KB flags NOT found in build script!" -ForegroundColor Red
}

Write-Host ""
Write-Host "STEP 2: Checking build environment..." -ForegroundColor Yellow

# Check for required tools
$tools = @(
    "C:\Program Files\Git\bin\bash.exe",
    "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358\toolchains\llvm\prebuilt\windows-x86_64\bin\clang.exe"
)

foreach ($tool in $tools) {
    if (Test-Path $tool) {
        Write-Host "✅ Found: $tool" -ForegroundColor Green
    } else {
        Write-Host "❌ Missing: $tool" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "STEP 3: Checking build directories..." -ForegroundColor Yellow

$dirs = @(
    "$gdalDir/cpp",
    "$gdalDir/libs",
    "$gdalDir/cpp/.install",
    "$gdalDir/cpp/.build"
)

foreach ($dir in $dirs) {
    if (Test-Path $dir) {
        $files = Get-ChildItem -Path $dir -Recurse -ErrorAction SilentlyContinue | Measure-Object
        Write-Host "✅ $dir exists ($($files.Count) files)" -ForegroundColor Green
    } else {
        Write-Host "❌ $dir missing" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "STEP 4: Testing build script syntax..." -ForegroundColor Yellow

try {
    Push-Location $gdalDir
    $testOutput = & $bashPath -n "$buildScript" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Build script syntax is valid" -ForegroundColor Green
    } else {
        Write-Host "❌ Build script syntax error:" -ForegroundColor Red
        Write-Host $testOutput -ForegroundColor Gray
    }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "STEP 5: Checking for common issues..." -ForegroundColor Yellow

# Check for wget
$wgetTest = & $bashPath -c "which wget" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ wget available" -ForegroundColor Green
} else {
    Write-Host "❌ wget NOT available - this is likely the issue!" -ForegroundColor Red
    Write-Host "   Build script needs wget to download dependencies" -ForegroundColor Gray
}

# Check for make/ninja
$makeTest = & $bashPath -c "which make" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ make available" -ForegroundColor Green
} else {
    Write-Host "❌ make NOT available" -ForegroundColor Red
}

$ninjaTest = & $bashPath -c "which ninja" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ ninja available" -ForegroundColor Green
} else {
    Write-Host "❌ ninja NOT available" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== DIAGNOSIS COMPLETE ===" -ForegroundColor Green

Write-Host ""
Write-Host "MOST LIKELY ISSUES:" -ForegroundColor Yellow
Write-Host "1. wget not available (needed to download GDAL dependencies)" -ForegroundColor White
Write-Host "2. make/ninja not available (needed to build)" -ForegroundColor White
Write-Host "3. Build script failed during dependency download" -ForegroundColor White

Write-Host ""
Write-Host "RECOMMENDATIONS:" -ForegroundColor Yellow
Write-Host "1. Install wget: 'choco install wget' or download manually" -ForegroundColor White
Write-Host "2. Use cmake-based build instead of make/ninja" -ForegroundColor White
Write-Host "3. Download dependencies manually and place in cpp directory" -ForegroundColor White
