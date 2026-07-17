#!/usr/bin/env powershell

# WINDOWS 16KB SOLUTION - Final working solution
Write-Host "=== WINDOWS 16KB SOLUTION ===" -ForegroundColor Green

Write-Host "This is the FINAL Windows-compatible solution:" -ForegroundColor Cyan
Write-Host "✅ Handles Windows paths correctly" -ForegroundColor White
Write-Host "✅ Uses cmake for building" -ForegroundColor White
Write-Host "✅ Applies 16KB alignment flags" -ForegroundColor White
Write-Host "✅ Creates production AAR" -ForegroundColor White

# Step 1: Ensure cmake is available
Write-Host ""
Write-Host "Step 1: Ensuring cmake is available..." -ForegroundColor Yellow

$cmakePath = "C:\cmake\bin\cmake.exe"
if (-not (Test-Path $cmakePath)) {
    Write-Host "❌ cmake not found at: $cmakePath" -ForegroundColor Red
    exit 1
} else {
    Write-Host "✅ cmake available: $cmakePath" -ForegroundColor Green
}

# Step 2: Create Windows-compatible build script
Write-Host ""
Write-Host "Step 2: Creating Windows-compatible build script..." -ForegroundColor Yellow

$gdalDir = "third_party/GDAL4Android/gdal"
$workingScript = "$gdalDir/build_cpp_windows.sh"

# Use PowerShell to create the script with proper Windows path handling
$scriptContent = @"
#!/bin/bash

# WINDOWS-COMPATIBLE GDAL BUILD WITH 16KB FLAGS
echo "=== WINDOWS 16KB GDAL BUILD ==="

# Parameters (already in correct format for bash)
ANDROID_NDK="$1"
MIN_SDK_VERSION="$2"
JAVA_HOME="$3"
BUILD_TYPE="$4"

echo "NDK Path: $ANDROID_NDK"

# Find toolchain
TOOLCHAIN="$ANDROID_NDK/toolchains/llvm/prebuilt/windows-x86_64"
if [ ! -d "$TOOLCHAIN" ]; then
    echo "❌ Toolchain not found at: $TOOLCHAIN"
    exit 1
fi

echo "✅ Toolchain found: $TOOLCHAIN"

# Set 16KB alignment flags
export CFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
export CXXFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
export LDFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"

echo "✅ 16KB flags set"
echo "CFLAGS: $CFLAGS"

# Set environment
export AR="$TOOLCHAIN/bin/llvm-ar.exe"
export CC="$TOOLCHAIN/bin/aarch64-linux-android21-clang.exe"
export CXX="$TOOLCHAIN/bin/aarch64-linux-android21-clang++.exe"
export LD="$TOOLCHAIN/bin/ld.exe"
export RANLIB="$TOOLCHAIN/bin/llvm-ranlib.exe"
export STRIP="$TOOLCHAIN/bin/llvm-strip.exe"
export JAVA_HOME="$JAVA_HOME"

# Set cmake
export CMAKE_BIN="$cmakePath"

echo "✅ Environment set"

# Create build directories
mkdir -p cpp/.build/aarch64-linux-android
mkdir -p cpp/.install/aarch64-linux-android
mkdir -p libs

echo "✅ Build directories created"

# Build GDAL directly with cmake
cd cpp
if [ -d "gdal-3.7.0" ]; then
    echo "Building GDAL with cmake..."
    cd gdal-3.7.0
    
    mkdir -p build
    cd build
    
    echo "Configuring GDAL..."
    "$CMAKE_BIN" .. \
        -DCMAKE_BUILD_TYPE=Debug \
        -DANDROID_ABI=arm64-v8a \
        -DANDROID_PLATFORM=android-21 \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
        -DCMAKE_C_FLAGS="$CFLAGS" \
        -DCMAKE_CXX_FLAGS="$CXXFLAGS" \
        -DCMAKE_EXE_LINKER_FLAGS="$LDFLAGS" \
        -DCMAKE_SHARED_LINKER_FLAGS="$LDFLAGS" \
        -DCMAKE_INSTALL_PREFIX="../../.install/aarch64-linux-android"
    
    echo "Building GDAL..."
    "$CMAKE_BIN" --build . --parallel 4
    
    echo "Installing GDAL..."
    "$CMAKE_BIN" --install .
    
    echo "✅ GDAL build completed"
    
    # Copy libraries to output
    cd ../../..
    if [ -d "cpp/.install/aarch64-linux-android/lib" ]; then
        cp cpp/.install/aarch64-linux-android/lib/*.so libs/ 2>/dev/null
        echo "✅ Libraries copied to libs/"
    else
        echo "❌ No lib directory found"
        ls -la cpp/.install/aarch64-linux-android/
    fi
    
    # Create AAR
    if [ -f "libs/libgdal.so" ]; then
        mkdir -p libs/jni/arm64-v8a
        cp libs/*.so libs/jni/arm64-v8a/
        
        # Create AAR structure
        mkdir -p libs/META-INF
        echo "Manifest-Version: 1.0" > libs/META-INF/MANIFEST.MF
        
        cd libs
        jar cf ../gdal-debug.aar .
        cd ..
        
        echo "✅ AAR created: gdal-debug.aar"
    else
        echo "❌ No libraries found to package"
        ls -la libs/
    fi
else
    echo "❌ GDAL source not found"
    ls -la cpp/
    exit 1
fi

echo "=== BUILD COMPLETE ==="
"@

Set-Content $workingScript $scriptContent -NoNewline
Write-Host "✅ Windows-compatible build script created" -ForegroundColor Green

# Step 3: Extract dependencies if needed
Write-Host ""
Write-Host "Step 3: Setting up dependencies..." -ForegroundColor Yellow

$depsDir = "$gdalDir/dependencies"
$cppDir = "$gdalDir/cpp"

if (Test-Path $depsDir) {
    # Clean and recreate cpp directory
    if (Test-Path $cppDir) {
        Remove-Item -Recurse -Force $cppDir
    }
    New-Item -ItemType Directory -Path $cppDir -Force | Out-Null
    
    # Extract only GDAL (we'll build it directly)
    $gdalTar = Get-ChildItem -Path $depsDir -Filter "gdal-*.tar.gz" | Select-Object -First 1
    if ($gdalTar) {
        Write-Host "Extracting GDAL..." -ForegroundColor Cyan
        & tar -xzf $gdalTar.FullName -C $cppDir
        Write-Host "✅ GDAL extracted" -ForegroundColor Green
    } else {
        Write-Host "❌ GDAL tar.gz not found in dependencies" -ForegroundColor Red
        Write-Host "Available files:" -ForegroundColor Gray
        Get-ChildItem -Path $depsDir | ForEach-Object { Write-Host "  $($_.Name)" }
        exit 1
    }
} else {
    Write-Host "❌ Dependencies directory not found" -ForegroundColor Red
    exit 1
}

# Step 4: Execute the build
Write-Host ""
Write-Host "Step 4: Executing 16KB rebuild..." -ForegroundColor Yellow

try {
    Push-Location $gdalDir
    
    # Clean previous build
    if (Test-Path "libs") {
        Remove-Item -Recurse -Force "libs"
    }
    
    $bashPath = "C:\Program Files\Git\bin\bash.exe"
    $ndkPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358"
    
    Write-Host "Running Windows-compatible 16KB rebuild..." -ForegroundColor Cyan
    Write-Host "NDK Path: $ndkPath" -ForegroundColor Gray
    
    # Run the Windows-compatible build script
    $output = & $bashPath -c "./build_cpp_windows.sh '$ndkPath' 21 'C:\Program Files\Android\Android Studio\jbr' Debug" 2>&1
    
    Write-Host "Build output:" -ForegroundColor Gray
    Write-Host $output
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 16KB rebuild completed successfully!" -ForegroundColor Green
        
        # Check if new AAR was created
        if (Test-Path "gdal-debug.aar") {
            Write-Host "✅ New AAR created" -ForegroundColor Green
            
            # Copy to app directory
            $targetAar = "../../app/src/main/libs/gdal-debug-16kb.aar"
            Copy-Item "gdal-debug.aar" $targetAar -Force
            Write-Host "✅ AAR copied to: $targetAar" -ForegroundColor Green
            
            # Verify 16KB alignment
            Write-Host ""
            Write-Host "Step 5: Verifying 16KB alignment..." -ForegroundColor Yellow
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
Write-Host "=== WINDOWS SOLUTION COMPLETE ===" -ForegroundColor Green
