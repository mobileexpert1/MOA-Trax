#!/usr/bin/env powershell

# MANUAL 16KB REBUILD - Step by Step
Write-Host "=== MANUAL 16KB REBUILD ===" -ForegroundColor Green

Write-Host "FOLLOW THESE EXACT STEPS:" -ForegroundColor Yellow

Write-Host ""
Write-Host "STEP 1: Open Git Bash" -ForegroundColor Cyan
Write-Host "• Right-click in Windows" -ForegroundColor White
Write-Host "• Select 'Git Bash Here'" -ForegroundColor White

Write-Host ""
Write-Host "STEP 2: Navigate to GDAL directory" -ForegroundColor Cyan
Write-Host "cd 'd:/AndroidStudioProjects/MOA-Trax/MOA-Trax-Mainbackup/Update_16_memory_support/MOA-Trax/third_party/GDAL4Android/gdal'" -ForegroundColor White

Write-Host ""
Write-Host "STEP 3: Clean previous build" -ForegroundColor Cyan
Write-Host "rm -rf cpp libs" -ForegroundColor White

Write-Host ""
Write-Host "STEP 4: Run rebuild with 16KB flags" -ForegroundColor Cyan
Write-Host "./build_cpp.sh 'C:\Users\Honey\AppData\Local\Android\Sdk\ndk\28.2.13676358' 21 'C:\Program Files\Android\Android Studio\jbr' Debug" -ForegroundColor White

Write-Host ""
Write-Host "STEP 5: Wait for completion (15-30 minutes)" -ForegroundColor Cyan
Write-Host "• This will download dependencies" -ForegroundColor White
Write-Host "• Build all libraries with 16KB flags" -ForegroundColor White
Write-Host "• Create new AAR with proper alignment" -ForegroundColor White

Write-Host ""
Write-Host "STEP 6: Copy result to app" -ForegroundColor Cyan
Write-Host "cp libs/gdal-debug.aar ../../app/src/main/libs/gdal-debug-16kb.aar" -ForegroundColor White

Write-Host ""
Write-Host "STEP 7: Verify alignment" -ForegroundColor Cyan
Write-Host "cd ../../scripts" -ForegroundColor White
Write-Host "powershell -ExecutionPolicy Bypass -File check-load-segments.ps1" -ForegroundColor White

Write-Host ""
Write-Host "🎯 EXPECTED RESULT:" -ForegroundColor Green
Write-Host "✅ All libraries show: Align 0x4000 (16KB)" -ForegroundColor White
Write-Host "✅ App builds and runs normally" -ForegroundColor White
Write-Host "✅ No functionality broken" -ForegroundColor White

Write-Host ""
Write-Host "⚠️  IF ERRORS OCCUR:" -ForegroundColor Yellow
Write-Host "• 'wget not found': Install wget or use alternative download method" -ForegroundColor White
Write-Host "• 'make not found': The script should use ninja automatically" -ForegroundColor White
Write-Host "• 'permission denied': Run as administrator" -ForegroundColor White

Write-Host ""
Write-Host "🚀 THIS IS THE SIMPLEST APPROACH:" -ForegroundColor Green
Write-Host "• Uses existing build script (already has 16KB flags)" -ForegroundColor White
Write-Host "• No additional setup required" -ForegroundColor White
Write-Host "• Step-by-step commands provided" -ForegroundColor White
Write-Host "• Guaranteed to work if dependencies are available" -ForegroundColor White

Write-Host ""
Write-Host "=== READY TO START ===" -ForegroundColor Green
Write-Host "Copy and paste the commands above in order." -ForegroundColor Yellow
