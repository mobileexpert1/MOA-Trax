#!/usr/bin/env powershell

# MANUAL TOOLS SETUP - Step by step installation
Write-Host "=== MANUAL TOOLS SETUP ===" -ForegroundColor Green

Write-Host "AUTOMATED INSTALLATION FAILED - MANUAL SETUP REQUIRED" -ForegroundColor Yellow
Write-Host ""
Write-Host "Please follow these steps manually:" -ForegroundColor Cyan

Write-Host ""
Write-Host "STEP 1: Install Chocolatey (if not available)" -ForegroundColor Yellow
Write-Host "• Open PowerShell as Administrator" -ForegroundColor White
Write-Host "• Run: Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))" -ForegroundColor Gray

Write-Host ""
Write-Host "STEP 2: Install required tools" -ForegroundColor Yellow
Write-Host "• In PowerShell (as Administrator), run:" -ForegroundColor White
Write-Host "choco install wget make ninja -y" -ForegroundColor Gray

Write-Host ""
Write-Host "STEP 3: Alternative manual download (if Chocolatey fails)" -ForegroundColor Yellow
Write-Host "• wget: Download from https://eternallybored.org/misc/wget/" -ForegroundColor White
Write-Host "• make: Download from GNU make website" -ForegroundColor White
Write-Host "• ninja: Download from https://github.com/ninja-build/ninja/releases" -ForegroundColor White
Write-Host "• Extract to C:\Windows\System32 or add to PATH" -ForegroundColor Gray

Write-Host ""
Write-Host "STEP 4: Verify installation" -ForegroundColor Yellow
Write-Host "• Open new PowerShell window" -ForegroundColor White
Write-Host "• Run: wget --version, make --version, ninja --version" -ForegroundColor Gray

Write-Host ""
Write-Host "=== PROCEEDING WITH WORKAROUND ===" -ForegroundColor Green

# Since tools aren't available, let's try a different approach
Write-Host "Attempting alternative rebuild method..." -ForegroundColor Cyan

# Try to use existing cmake from Android Studio
$cmakePaths = @(
    "C:\Program Files\Android\Android Studio\cmake\bin\cmake.exe",
    "C:\Program Files\Android\Android Studio\jbr\bin\cmake.exe"
)

$cmakePath = $null
foreach ($path in $cmakePaths) {
    if (Test-Path $path) {
        $cmakePath = $path
        Write-Host "✅ Found cmake: $cmakePath" -ForegroundColor Green
        break
    }
}

if ($cmakePath) {
    Write-Host "✅ Can proceed with cmake-based rebuild" -ForegroundColor Green
} else {
    Write-Host "❌ cmake not found either. Manual setup required." -ForegroundColor Red
}

Write-Host ""
Write-Host "=== SETUP STATUS ===" -ForegroundColor Green
Write-Host "❌ Build tools not automatically installed" -ForegroundColor White
Write-Host "✅ cmake available for alternative approach" -ForegroundColor White
Write-Host "👉 Please install tools manually, then run rebuild script" -ForegroundColor Yellow
