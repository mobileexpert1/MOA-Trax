#!/usr/bin/env powershell

# CMAKE-BASED 16KB REBUILD - No wget/make/ninja required
Write-Host "=== CMAKE-BASED 16KB REBUILD ===" -ForegroundColor Green

Write-Host "This approach uses cmake instead of make/ninja:" -ForegroundColor Cyan
Write-Host "✅ No wget required (dependencies downloaded manually)" -ForegroundColor White
Write-Host "✅ No make required (uses cmake)" -ForegroundColor White
Write-Host "✅ No ninja required (uses cmake)" -ForegroundColor White

# Step 1: Find cmake
Write-Host ""
Write-Host "Step 1: Finding cmake..." -ForegroundColor Yellow

$cmakePath = $null
$possiblePaths = @(
    "C:\Program Files\Android\Android Studio\cmake\bin\cmake.exe",
    "C:\Program Files\Android\Android Studio\jbr\bin\cmake.exe",
    "C:\Program Files\CMake\bin\cmake.exe"
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
        Write-Host "❌ cmake not found. Please install cmake manually." -ForegroundColor Red
        exit 1
    }
}

# Step 2: Download dependencies manually
Write-Host ""
Write-Host "Step 2: Downloading GDAL dependencies..." -ForegroundColor Yellow

$gdalDir = "third_party/GDAL4Android/gdal"
$depsDir = "$gdalDir/dependencies"

if (-not (Test-Path $depsDir)) {
    New-Item -ItemType Directory -Path $depsDir -Force | Out-Null
}

$dependencies = @{
    "sqlite" = "https://www.sqlite.org/2024/sqlite-autoconf-3420000.tar.gz"
    "proj" = "https://download.osgeo.org/proj/proj-9.2.1.tar.gz"
    "gdal" = "https://download.osgeo.org/gdal/3.7.0/gdal-3.7.0.tar.gz"
    "expat" = "https://github.com/libexpat/libexpat/releases/download/R_2_5_0/expat-2.5.0.tar.gz"
    "iconv" = "https://ftp.gnu.org/gnu/libiconv/libiconv-1.17.tar.gz"
}

Write-Host "Downloading dependencies..." -ForegroundColor Cyan
foreach ($dep in $dependencies.GetEnumerator()) {
    $tarFile = "$depsDir/$($dep.Value.Split('/')[-1])"
    if (-not (Test-Path $tarFile)) {
        Write-Host "Downloading $($dep.Key)..." -ForegroundColor Gray
        try {
            Invoke-WebRequest -Uri $dep.Value -OutFile $tarFile
            Write-Host "✅ Downloaded $($dep.Key)" -ForegroundColor Green
        } catch {
            Write-Host "❌ Failed to download $($dep.Key)" -ForegroundColor Red
        }
    } else {
        Write-Host "✅ $($dep.Key) already downloaded" -ForegroundColor Green
    }
}

# Step 3: Extract dependencies
Write-Host ""
Write-Host "Step 3: Extracting dependencies..." -ForegroundColor Yellow

$extractDir = "$gdalDir/cpp"
if (Test-Path $extractDir) {
    Remove-Item -Recurse -Force $extractDir
}
New-Item -ItemType Directory -Path $extractDir -Force | Out-Null

foreach ($dep in $dependencies.GetEnumerator()) {
    $tarFile = "$depsDir/$($dep.Value.Split('/')[-1])"
    if (Test-Path $tarFile) {
        Write-Host "Extracting $($dep.Key)..." -ForegroundColor Gray
        try {
            # Use tar (available in Windows 10+)
            & tar -xzf $tarFile -C $extractDir
            Write-Host "✅ Extracted $($dep.Key)" -ForegroundColor Green
        } catch {
            Write-Host "❌ Failed to extract $($dep.Key)" -ForegroundColor Red
        }
    }
}

# Step 4: Create cmake-based build script
Write-Host ""
Write-Host "Step 4: Creating cmake-based build..." -ForegroundColor Yellow

$ndkPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358"
$buildScript = @"
#!/bin/bash
# CMAKE-BASED GDAL REBUILD WITH 16KB FLAGS

echo "=== CMAKE 16KB REBUILD ==="

# Set 16KB alignment flags
export CFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
export CXXFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
export LDFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"

echo "✅ 16KB flags set"

cd "$extractDir"

# Build for arm64-v8a
mkdir -p build_arm64
cd build_arm64

"$cmakePath" ../gdal-3.7.0 \
    -DCMAKE_BUILD_TYPE=Debug \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-21 \
    -DCMAKE_TOOLCHAIN_FILE="$ndkPath/build/cmake/android.toolchain.cmake" \
    -DCMAKE_C_FLAGS="$CFLAGS" \
    -DCMAKE_CXX_FLAGS="$CXXFLAGS" \
    -DCMAKE_EXE_LINKER_FLAGS="$LDFLAGS" \
    -DCMAKE_SHARED_LINKER_FLAGS="$LDFLAGS"

"$cmakePath" --build . --parallel 4

echo "✅ CMAKE build completed"
"@

$scriptPath = Join-Path $env:TEMP "cmake_build.sh"
Set-Content $scriptPath $buildScript

# Step 5: Execute cmake build
Write-Host ""
Write-Host "Step 5: Executing cmake build..." -ForegroundColor Yellow

try {
    Push-Location $gdalDir
    
    $bashPath = "C:\Program Files\Git\bin\bash.exe"
    Write-Host "Running cmake-based rebuild..." -ForegroundColor Cyan
    
    $output = & $bashPath $scriptPath 2>&1
    Write-Host "Build output:" -ForegroundColor Gray
    Write-Host $output
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ CMAKE rebuild completed!" -ForegroundColor Green
        
        # Check for built libraries
        $buildDir = "$extractDir/build_arm64"
        if (Test-Path $buildDir) {
            $soFiles = Get-ChildItem -Path $buildDir -Recurse -Filter "*.so"
            Write-Host "Built libraries:" -ForegroundColor Cyan
            $soFiles | ForEach-Object {
                Write-Host "  $($_.Name)" -ForegroundColor White
            }
        }
    } else {
        Write-Host "⚠️  CMAKE build had issues" -ForegroundColor Yellow
    }
    
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== CMAKE REBUILD COMPLETE ===" -ForegroundColor Green
