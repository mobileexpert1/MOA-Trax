#!/usr/bin/env powershell

# PRODUCTION 16KB REBUILD - Complete solution
Write-Host "=== PRODUCTION 16KB REBUILD ===" -ForegroundColor Green

Write-Host "This script provides a complete production-ready 16KB solution:" -ForegroundColor Cyan
Write-Host "✅ Downloads dependencies manually" -ForegroundColor White
Write-Host "✅ Uses existing build script with 16KB flags" -ForegroundColor White
Write-Host "✅ Verifies 16KB alignment" -ForegroundColor White
Write-Host "✅ Creates production AAR" -ForegroundColor White

# Step 1: Download dependencies manually
Write-Host ""
Write-Host "Step 1: Downloading GDAL dependencies..." -ForegroundColor Yellow

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

Write-Host "Downloading dependencies (this may take a while)..." -ForegroundColor Cyan
foreach ($dep in $dependencies.GetEnumerator()) {
    $tarFile = "$depsDir/$($dep.Value.Split('/')[-1])"
    if (-not (Test-Path $tarFile)) {
        Write-Host "Downloading $($dep.Key)..." -ForegroundColor Gray
        try {
            # Use Invoke-WebRequest with retry
            $maxRetries = 3
            $retryCount = 0
            $downloaded = $false
            
            while (-not $downloaded -and $retryCount -lt $maxRetries) {
                try {
                    Invoke-WebRequest -Uri $dep.Value -OutFile $tarFile -UseBasicParsing -TimeoutSec 300
                    $downloaded = $true
                    Write-Host "✅ Downloaded $($dep.Key)" -ForegroundColor Green
                } catch {
                    $retryCount++
                    Write-Host "⚠️  Retry $retryCount for $($dep.Key)..." -ForegroundColor Yellow
                    Start-Sleep 5
                }
            }
            
            if (-not $downloaded) {
                Write-Host "❌ Failed to download $($dep.Key)" -ForegroundColor Red
            }
        } catch {
            Write-Host "❌ Failed to download $($dep.Key)" -ForegroundColor Red
        }
    } else {
        Write-Host "✅ $($dep.Key) already downloaded" -ForegroundColor Green
    }
}

# Step 2: Extract dependencies to expected locations
Write-Host ""
Write-Host "Step 2: Extracting dependencies..." -ForegroundColor Yellow

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

# Step 3: Create a modified build script that doesn't need wget
Write-Host ""
Write-Host "Step 3: Creating modified build script..." -ForegroundColor Yellow

$originalScript = "$gdalDir/build_cpp.sh"
$modifiedScript = "$gdalDir/build_cpp_16kb.sh"

# Read original script and modify it to skip wget downloads
$scriptContent = Get-Content $originalScript -Raw

# Remove wget download lines (since we downloaded manually)
$scriptContent = $scriptContent -replace 'wget.*tar\.gz', '# wget line removed - dependencies downloaded manually'

# Add our 16KB flags (ensure they're there)
if ($scriptContent -notmatch "max-page-size=16384") {
    $scriptContent = $scriptContent -replace "export  LDFLAGS=`"\$LDFLAGS`"", "export  LDFLAGS=`"\$LDFLAGS -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384`""
}

Set-Content $modifiedScript $scriptContent -NoNewline
Write-Host "✅ Modified build script created" -ForegroundColor Green

# Step 4: Execute the rebuild
Write-Host ""
Write-Host "Step 4: Executing 16KB rebuild..." -ForegroundColor Yellow

try {
    Push-Location $gdalDir
    
    # Clean previous build
    if (Test-Path "cpp") {
        Remove-Item -Recurse -Force "cpp"
    }
    if (Test-Path "libs") {
        Remove-Item -Recurse -Force "libs"
    }
    
    # Copy dependencies back to cpp directory
    Copy-Item -Recurse -Force "$extractDir\*" "cpp\"
    
    $bashPath = "C:\Program Files\Git\bin\bash.exe"
    $ndkPath = "C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358"
    
    Write-Host "Running 16KB rebuild (this will take 15-30 minutes)..." -ForegroundColor Cyan
    
    # Run the modified build script
    $output = & $bashPath -c "./build_cpp_16kb.sh '$ndkPath' 21 'C:\Program Files\Android\Android Studio\jbr' Debug" 2>&1
    
    Write-Host "Build output:" -ForegroundColor Gray
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
            Write-Host "Step 5: Verifying 16KB alignment..." -ForegroundColor Yellow
            & ../../scripts/check-load-segments.ps1
        } else {
            Write-Host "❌ AAR not created" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ 16KB rebuild failed" -ForegroundColor Red
        Write-Host "Check the output above for error details" -ForegroundColor Gray
    }
    
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== PRODUCTION REBUILD COMPLETE ===" -ForegroundColor Green
