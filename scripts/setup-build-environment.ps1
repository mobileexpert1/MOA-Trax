#!/usr/bin/env powershell

# SETUP BUILD ENVIRONMENT - Install required tools
Write-Host "=== SETTING UP BUILD ENVIRONMENT ===" -ForegroundColor Green

Write-Host "This script will install the required tools for GDAL rebuild:" -ForegroundColor Cyan
Write-Host "• wget (for downloading dependencies)" -ForegroundColor White
Write-Host "• make (for building)" -ForegroundColor White
Write-Host "• ninja (for faster builds)" -ForegroundColor White

# Step 1: Check if Chocolatey is available
Write-Host ""
Write-Host "Step 1: Checking Chocolatey..." -ForegroundColor Yellow

try {
    $chocoVersion = choco --version 2>$null
    if ($chocoVersion) {
        Write-Host "✅ Chocolatey found: $chocoVersion" -ForegroundColor Green
    } else {
        Write-Host "❌ Chocolatey not found. Installing..." -ForegroundColor Red
        Set-ExecutionPolicy Bypass -Scope Process -Force
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
        iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
        Write-Host "✅ Chocolatey installed" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Error installing Chocolatey. Trying manual installation..." -ForegroundColor Red
}

# Step 2: Install wget
Write-Host ""
Write-Host "Step 2: Installing wget..." -ForegroundColor Yellow

try {
    $wgetTest = wget --version 2>$null
    if ($wgetTest) {
        Write-Host "✅ wget already available" -ForegroundColor Green
    } else {
        Write-Host "Installing wget via Chocolatey..." -ForegroundColor Cyan
        choco install wget -y
        Write-Host "✅ wget installed" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Error installing wget. Trying alternative..." -ForegroundColor Red
    # Download wget manually
    $wgetUrl = "https://eternallybored.org/misc/wget/current/wget.exe"
    $wgetPath = "C:\Windows\System32\wget.exe"
    try {
        Invoke-WebRequest -Uri $wgetUrl -OutFile $wgetPath
        Write-Host "✅ wget downloaded manually" -ForegroundColor Green
    } catch {
        Write-Host "❌ Failed to download wget" -ForegroundColor Red
    }
}

# Step 3: Install make
Write-Host ""
Write-Host "Step 3: Installing make..." -ForegroundColor Yellow

try {
    $makeTest = make --version 2>$null
    if ($makeTest) {
        Write-Host "✅ make already available" -ForegroundColor Green
    } else {
        Write-Host "Installing make via Chocolatey..." -ForegroundColor Cyan
        choco install make -y
        Write-Host "✅ make installed" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Error installing make" -ForegroundColor Red
}

# Step 4: Install ninja
Write-Host ""
Write-Host "Step 4: Installing ninja..." -ForegroundColor Yellow

try {
    $ninjaTest = ninja --version 2>$null
    if ($ninjaTest) {
        Write-Host "✅ ninja already available" -ForegroundColor Green
    } else {
        Write-Host "Installing ninja via Chocolatey..." -ForegroundColor Cyan
        choco install ninja -y
        Write-Host "✅ ninja installed" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Error installing ninja. Trying manual installation..." -ForegroundColor Red
    # Download ninja manually
    $ninjaUrl = "https://github.com/ninja-build/ninja/releases/download/v1.11.1/ninja-win.zip"
    $ninjaPath = "C:\Windows\System32\ninja.exe"
    $tempNinja = Join-Path $env:TEMP "ninja-win.zip"
    try {
        Invoke-WebRequest -Uri $ninjaUrl -OutFile $tempNinja
        Expand-Archive -Path $tempNinja -DestinationPath "C:\Windows\System32" -Force
        Remove-Item $tempNinja
        Write-Host "✅ ninja downloaded manually" -ForegroundColor Green
    } catch {
        Write-Host "❌ Failed to download ninja" -ForegroundColor Red
    }
}

# Step 5: Verify installations
Write-Host ""
Write-Host "Step 5: Verifying installations..." -ForegroundColor Yellow

$tools = @("wget", "make", "ninja")
$allInstalled = $true

foreach ($tool in $tools) {
    try {
        $version = & $tool --version 2>$null
        if ($version) {
            Write-Host "✅ $tool available" -ForegroundColor Green
        } else {
            Write-Host "❌ $tool not available" -ForegroundColor Red
            $allInstalled = $false
        }
    } catch {
        Write-Host "❌ $tool not available" -ForegroundColor Red
        $allInstalled = $false
    }
}

if ($allInstalled) {
    Write-Host ""
    Write-Host "✅ All required tools installed successfully!" -ForegroundColor Green
    Write-Host "Ready to proceed with GDAL rebuild." -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "❌ Some tools failed to install. Manual installation may be required." -ForegroundColor Red
}

Write-Host ""
Write-Host "=== ENVIRONMENT SETUP COMPLETE ===" -ForegroundColor Green
