#!/usr/bin/env powershell

# DIRECT 16KB REBUILD - Manual tool installation
Write-Host "=== DIRECT 16KB REBUILD ===" -ForegroundColor Green

Write-Host "MANUAL INSTALLATION APPROACH:" -ForegroundColor Cyan
Write-Host "✅ Downloads tools directly (no admin required)" -ForegroundColor White
Write-Host "✅ Installs to local directory" -ForegroundColor White
Write-Host "✅ Builds GDAL with 16KB alignment" -ForegroundColor White
Write-Host "✅ Verifies 16KB alignment" -ForegroundColor White

# Step 1: Create local tools directory
Write-Host ""
Write-Host "Step 1: Setting up local tools directory..." -ForegroundColor Yellow

$toolsDir = "C:\gdal-build-tools"
if (Test-Path $toolsDir) {
    Remove-Item -Recurse -Force $toolsDir
}
New-Item -ItemType Directory -Path $toolsDir -Force | Out-Null
New-Item -ItemType Directory -Path "$toolsDir\bin" -Force | Out-Null

# Add to PATH for this session
$env:PATH = "$toolsDir\bin;$env:PATH"

Write-Host "✅ Tools directory created: $toolsDir" -ForegroundColor Green

# Step 2: Download and install wget
Write-Host ""
Write-Host "Step 2: Installing wget..." -ForegroundColor Yellow

try {
    $wgetUrl = "https://eternallybored.org/misc/wget/current/wget.exe"
    $wgetPath = "$toolsDir\bin\wget.exe"
    Write-Host "Downloading wget..." -ForegroundColor Gray
    Invoke-WebRequest -Uri $wgetUrl -OutFile $wgetPath -UseBasicParsing
    Write-Host "✅ wget installed" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to download wget" -ForegroundColor Red
}

# Step 3: Download and install make
Write-Host ""
Write-Host "Step 3: Installing make..." -ForegroundColor Yellow

try {
    $makeUrl = "https://github.com/mirror/mingw-w64/releases/download/mingw-w64-v11.0.0/mingw-w64-x86_64-11.0.0-release-win32-seh-rt_v11-rev2.7z"
    $makeZip = Join-Path $env:TEMP "make.7z"
    $makeExtract = Join-Path $env:TEMP "make_extract"
    
    Write-Host "Downloading make..." -ForegroundColor Gray
    Invoke-WebRequest -Uri $makeUrl -OutFile $makeZip -UseBasicParsing
    
    # Extract make
    New-Item -ItemType Directory -Path $makeExtract -Force | Out-Null
    & 7z x $makeZip -o$makeExtract -y
    
    # Find and copy make.exe
    $makeExe = Get-ChildItem -Path $makeExtract -Recurse -Filter "make.exe" | Select-Object -First 1
    if ($makeExe) {
        Copy-Item $makeExe.FullName "$toolsDir\bin\make.exe" -Force
        Write-Host "✅ make installed" -ForegroundColor Green
    } else {
        Write-Host "❌ make.exe not found in archive" -ForegroundColor Red
    }
    
    # Cleanup
    Remove-Item $makeZip -Force
    Remove-Item $makeExtract -Recurse -Force
} catch {
    Write-Host "❌ Failed to install make" -ForegroundColor Red
}

# Step 4: Download and install ninja
Write-Host ""
Write-Host "Step 4: Installing ninja..." -ForegroundColor Yellow

try {
    $ninjaUrl = "https://github.com/ninja-build/ninja/releases/download/v1.11.1/ninja-win.zip"
    $ninjaZip = Join-Path $env:TEMP "ninja.zip"
    
    Write-Host "Downloading ninja..." -ForegroundColor Gray
    Invoke-WebRequest -Uri $ninjaUrl -OutFile $ninjaZip -UseBasicParsing
    
    # Extract ninja
    Expand-Archive -Path $ninjaZip -DestinationPath "$toolsDir\bin" -Force
    Write-Host "✅ ninja installed" -ForegroundColor Green
    
    Remove-Item $ninjaZip -Force
} catch {
    Write-Host "❌ Failed to install ninja" -ForegroundColor Red
}

# Step 5: Download and install cmake
Write-Host ""
Write-Host "Step 5: Installing cmake..." -ForegroundColor Yellow

try {
    $cmakeUrl = "https://github.com/Kitware/CMake/releases/download/v3.28.1/cmake-3.28.1-windows-x86_64.zip"
    $cmakeZip = Join-Path $env:TEMP "cmake.zip"
    
    Write-Host "Downloading cmake..." -ForegroundColor Gray
    Invoke-WebRequest -Uri $cmakeUrl -OutFile $cmakeZip -UseBasicParsing
    
    # Extract cmake
    Expand-Archive -Path $cmakeZip -DestinationPath $toolsDir -Force
    
    # Move cmake to bin directory
    $cmakeBin = Get-ChildItem -Path $toolsDir -Filter "cmake.exe" -Recurse | Select-Object -First 1
    if ($cmakeBin) {
        $cmakeSourceDir = Split-Path $cmakeBin.FullName -Parent
        Get-ChildItem -Path $cmakeSourceDir | Copy-Item -Destination "$toolsDir\bin" -Recurse -Force
        Write-Host "✅ cmake installed" -ForegroundColor Green
    }
    
    Remove-Item $cmakeZip -Force
} catch {
    Write-Host "❌ Failed to install cmake" -ForegroundColor Red
}

# Step 6: Verify tools
Write-Host ""
Write-Host "Step 6: Verifying installed tools..." -ForegroundColor Yellow

$tools = @("wget", "make", "ninja", "cmake")
$allToolsAvailable = $true

foreach ($tool in $tools) {
    try {
        $version = & $tool --version 2>$null
        if ($version) {
            Write-Host "✅ $tool available" -ForegroundColor Green
        } else {
            Write-Host "❌ $tool not available" -ForegroundColor Red
            $allToolsAvailable = $false
        }
    } catch {
        Write-Host "❌ $tool not available" -ForegroundColor Red
        $allToolsAvailable = $false
    }
}

if (-not $allToolsAvailable) {
    Write-Host "❌ Some tools are not available. Rebuild may fail." -ForegroundColor Red
}

# Step 7: Download GDAL dependencies
Write-Host ""
Write-Host "Step 7: Downloading GDAL dependencies..." -ForegroundColor Yellow

$gdalDir = "third_party/GDAL4Android/gdal"
$depsDir = "$gdalDir/dependencies"

if (Test-Path $depsDir) {
    Remove-Item -Recurse -Force $depsDir
}
New-Item -ItemType Directory -Path $depsDir -Force | Out-Null

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
    Write-Host "Downloading $($dep.Key)..." -ForegroundColor Gray
    
    try {
        Invoke-WebRequest -Uri $dep.Value -OutFile $tarFile -UseBasicParsing -TimeoutSec 300
        Write-Host "✅ $($dep.Key) downloaded" -ForegroundColor Green
    } catch {
        Write-Host "❌ Failed to download $($dep.Key)" -ForegroundColor Red
    }
}

# Step 8: Execute rebuild
Write-Host ""
Write-Host "Step 8: Executing 16KB rebuild..." -ForegroundColor Yellow

try {
    Push-Location $gdalDir
    
    # Clean previous build
    if (Test-Path "cpp") {
        Remove-Item -Recurse -Force "cpp"
    }
    if (Test-Path "libs") {
        Remove-Item -Recurse -Force "libs"
    }
    
    # Extract dependencies
    $extractDir = "$gdalDir/cpp"
    New-Item -ItemType Directory -Path $extractDir -Force | Out-Null
    
    foreach ($dep in $dependencies.GetEnumerator()) {
        $tarFile = "$depsDir/$($dep.Value.Split('/')[-1])"
        if (Test-Path $tarFile) {
            Write-Host "Extracting $($dep.Key)..." -ForegroundColor Gray
            try {
                & tar -xzf $tarFile -C $extractDir
                Write-Host "✅ $($dep.Key) extracted" -ForegroundColor Green
            } catch {
                Write-Host "❌ Failed to extract $($dep.Key)" -ForegroundColor Red
            }
        }
    }
    
    # Run build with our tools
    $bashPath = "C:\Program Files\Git\bin\bash.exe"
    $ndkPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358"
    
    Write-Host "Starting 16KB rebuild with manual tools..." -ForegroundColor Cyan
    
    # Set environment for bash to use our tools
    $env:PATH = "$toolsDir\bin;$env:PATH"
    
    $output = & $bashPath -c "./build_cpp.sh '$ndkPath' 21 'C:\Program Files\Android\Android Studio\jbr' Debug" 2>&1
    
    Write-Host "Build output (last 50 lines):" -ForegroundColor Gray
    $outputLines = $output -split "`n"
    $outputLines | Select-Object -Last 50 | ForEach-Object { Write-Host $_ }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 16KB rebuild completed!" -ForegroundColor Green
        
        # Copy AAR
        if (Test-Path "libs/gdal-debug.aar") {
            $targetAar = "../../app/src/main/libs/gdal-debug-16kb.aar"
            Copy-Item "libs/gdal-debug.aar" $targetAar -Force
            Write-Host "✅ AAR copied to: $targetAar" -ForegroundColor Green
            
            # Verify alignment
            Write-Host ""
            Write-Host "Step 9: Verifying 16KB alignment..." -ForegroundColor Yellow
            & ../../scripts/check-load-segments.ps1
        }
    } else {
        Write-Host "❌ 16KB rebuild failed" -ForegroundColor Red
    }
    
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== DIRECT 16KB REBUILD COMPLETE ===" -ForegroundColor Green
