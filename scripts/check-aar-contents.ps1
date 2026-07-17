Add-Type -AssemblyName System.IO.Compression.FileSystem

$zip = [System.IO.Compression.ZipFile]::OpenRead('app/src/main/libs/gdal-debug-16kb.aar')
$entries = $zip.Entries | Where-Object {$_.FullName -like '*.class'} | Select-Object -First 10
foreach ($entry in $entries) {
    Write-Host $entry.FullName
}
$zip.Dispose()
