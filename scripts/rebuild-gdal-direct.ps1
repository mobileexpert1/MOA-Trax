#!/usr/bin/env powershell

# REBUILD GDAL DIRECTLY WITH 16KB FLAGS
Write-Host "=== REBUILDING GDAL WITH 16KB FLAGS ===" -ForegroundColor Green

$gdalDir = "third_party/GDAL4Android/gdal"
$bashPath = "C:\Program Files\Git\bin\bash.exe"
$buildScript = "build_cpp.sh"

if (-not (Test-Path $bashPath)) {
    Write-Error "Git Bash not found at: $bashPath"
    exit 1
}

if (-not (Test-Path "$gdalDir/$buildScript")) {
    Write-Error "GDAL build script not found: $gdalDir/$buildScript"
    exit 1
}

try {
    Write-Host "Starting GDAL rebuild with 16KB alignment flags..." -ForegroundColor Cyan
    
    # Change to GDAL directory and run build script
    Push-Location $gdalDir
    
    Write-Host "Running: bash $buildScript" -ForegroundColor Yellow
    $output = & $bashPath $buildScript 2>&1
    
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
    } else {
        Write-Error "❌ GDAL rebuild failed with exit code: $LASTEXITCODE"
        exit 1
    }
    
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== REBUILD COMPLETE ===" -ForegroundColor Green
