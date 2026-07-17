Add-Type -AssemblyName System.IO.Compression.FileSystem

Write-Host "=== Comparing AAR Contents ===" -ForegroundColor Green

$originalAar = "app/src/main/libs/gdal-debug.aar"
$alignedAar = "app/src/main/libs/gdal-debug-16kb.aar"

Write-Host "Original AAR contents:" -ForegroundColor Yellow
$zip1 = [System.IO.Compression.ZipFile]::OpenRead($originalAar)
$entries1 = $zip1.Entries | Where-Object {$_.FullName -like '*.class' -or $_.FullName -like '*.jar'} | Select-Object -First 5
foreach ($entry in $entries1) {
    Write-Host "  $($entry.FullName)"
}
$zip1.Dispose()

Write-Host "16KB AAR contents:" -ForegroundColor Yellow
$zip2 = [System.IO.Compression.ZipFile]::OpenRead($alignedAar)
$entries2 = $zip2.Entries | Where-Object {$_.FullName -like '*.class' -or $_.FullName -like '*.jar'} | Select-Object -First 5
foreach ($entry in $entries2) {
    Write-Host "  $($entry.FullName)"
}
$zip2.Dispose()

Write-Host "All entries in 16KB AAR:" -ForegroundColor Cyan
$zip3 = [System.IO.Compression.ZipFile]::OpenRead($alignedAar)
$allEntries = $zip3.Entries | Sort-Object FullName
foreach ($entry in $allEntries) {
    Write-Host "  $($entry.FullName)"
}
$zip3.Dispose()
