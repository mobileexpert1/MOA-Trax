Add-Type -AssemblyName System.IO.Compression.FileSystem

Write-Host "=== Checking GDAL Classes ===" -ForegroundColor Green

$alignedAar = "app/src/main/libs/gdal-debug-16kb.aar"
$tempDir = Join-Path $env:TEMP "check_classes_$(Get-Random)"

# Extract AAR
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
[System.IO.Compression.ZipFile]::ExtractToDirectory($alignedAar, $tempDir)

# Check classes.jar
$classesJar = Join-Path $tempDir "classes.jar"
if (Test-Path $classesJar) {
    Write-Host "Found classes.jar, checking contents..." -ForegroundColor Cyan
    
    # Extract classes.jar
    $classesDir = Join-Path $tempDir "classes"
    New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
    [System.IO.Compression.ZipFile]::ExtractToDirectory($classesJar, $classesDir)
    
    # Look for GDAL classes
    $gdalClasses = Get-ChildItem -Path $classesDir -Filter "*gdal*" -Recurse
    Write-Host "GDAL-related classes found:" -ForegroundColor Yellow
    foreach ($class in $gdalClasses) {
        Write-Host "  $($class.FullName.Replace($classesDir, ''))"
    }
    
    # Also check libs/gdal.jar
    $libsJar = Join-Path $tempDir "libs\gdal.jar"
    if (Test-Path $libsJar) {
        Write-Host "Checking libs/gdal.jar..." -ForegroundColor Cyan
        
        $libsDir = Join-Path $tempDir "libs_gdal"
        New-Item -ItemType Directory -Path $libsDir -Force | Out-Null
        [System.IO.Compression.ZipFile]::ExtractToDirectory($libsJar, $libsDir)
        
        $libGdalClasses = Get-ChildItem -Path $libsDir -Filter "*.class" -Recurse
        Write-Host "GDAL classes in libs/gdal.jar:" -ForegroundColor Yellow
        foreach ($class in $libGdalClasses | Select-Object -First 10) {
            Write-Host "  $($class.FullName.Replace($libsDir, ''))"
        }
        Write-Host "  ... and $($(Get-ChildItem -Path $libsDir -Filter "*.class" -Recurse).Count) total classes"
    }
} else {
    Write-Host "No classes.jar found!" -ForegroundColor Red
}

# Cleanup
Remove-Item -Path $tempDir -Recurse -Force
