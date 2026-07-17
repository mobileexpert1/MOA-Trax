param(
    [Parameter(Mandatory = $true)]
    [string]$ArtifactPath,
    [string]$Label = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-UInt16LEOrBE([byte[]]$bytes, [int]$offset, [bool]$littleEndian) {
    if ($littleEndian) {
        return [BitConverter]::ToUInt16($bytes, $offset)
    }
    $tmp = $bytes[$offset..($offset + 1)]
    [Array]::Reverse($tmp)
    return [BitConverter]::ToUInt16($tmp, 0)
}

function Get-UInt32LEOrBE([byte[]]$bytes, [int]$offset, [bool]$littleEndian) {
    if ($littleEndian) {
        return [BitConverter]::ToUInt32($bytes, $offset)
    }
    $tmp = $bytes[$offset..($offset + 3)]
    [Array]::Reverse($tmp)
    return [BitConverter]::ToUInt32($tmp, 0)
}

function Get-UInt64LEOrBE([byte[]]$bytes, [int]$offset, [bool]$littleEndian) {
    if ($littleEndian) {
        return [BitConverter]::ToUInt64($bytes, $offset)
    }
    $tmp = $bytes[$offset..($offset + 7)]
    [Array]::Reverse($tmp)
    return [BitConverter]::ToUInt64($tmp, 0)
}

function Get-PtLoadAlignments([byte[]]$bytes) {
    if ($bytes.Length -lt 64) {
        return @()
    }
    if ($bytes[0] -ne 0x7f -or $bytes[1] -ne 0x45 -or $bytes[2] -ne 0x4c -or $bytes[3] -ne 0x46) {
        return @()
    }

    $elfClass = $bytes[4] # 1 = 32-bit, 2 = 64-bit
    $littleEndian = ($bytes[5] -eq 1)
    $alignments = New-Object System.Collections.Generic.List[UInt64]

    if ($elfClass -eq 2) {
        $phoff = [int](Get-UInt64LEOrBE $bytes 32 $littleEndian)
        $phentsize = [int](Get-UInt16LEOrBE $bytes 54 $littleEndian)
        $phnum = [int](Get-UInt16LEOrBE $bytes 56 $littleEndian)
        for ($i = 0; $i -lt $phnum; $i++) {
            $off = $phoff + ($i * $phentsize)
            if ($off + 56 -gt $bytes.Length) { break }
            $pType = Get-UInt32LEOrBE $bytes $off $littleEndian
            if ($pType -eq 1) { # PT_LOAD
                $pAlign = Get-UInt64LEOrBE $bytes ($off + 48) $littleEndian
                [void]$alignments.Add($pAlign)
            }
        }
    } elseif ($elfClass -eq 1) {
        $phoff = [int](Get-UInt32LEOrBE $bytes 28 $littleEndian)
        $phentsize = [int](Get-UInt16LEOrBE $bytes 42 $littleEndian)
        $phnum = [int](Get-UInt16LEOrBE $bytes 44 $littleEndian)
        for ($i = 0; $i -lt $phnum; $i++) {
            $off = $phoff + ($i * $phentsize)
            if ($off + 32 -gt $bytes.Length) { break }
            $pType = Get-UInt32LEOrBE $bytes $off $littleEndian
            if ($pType -eq 1) { # PT_LOAD
                $pAlign = [UInt64](Get-UInt32LEOrBE $bytes ($off + 28) $littleEndian)
                [void]$alignments.Add($pAlign)
            }
        }
    }

    return $alignments
}

$resolved = Resolve-Path $ArtifactPath
if (-not $resolved) {
    throw "Artifact not found: $ArtifactPath"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($resolved)
try {
    $bad = New-Object System.Collections.Generic.List[string]
    $checked = 0

    foreach ($entry in $zip.Entries) {
        if (-not ($entry.FullName -like "lib/*.so" -or $entry.FullName -like "jni/*.so")) {
            continue
        }

        $ms = New-Object System.IO.MemoryStream
        $stream = $entry.Open()
        try {
            $stream.CopyTo($ms)
        } finally {
            $stream.Close()
        }
        $bytes = $ms.ToArray()
        $alignments = Get-PtLoadAlignments $bytes
        if ($alignments.Count -eq 0) {
            continue
        }
        $checked++
        $isBad = $false
        foreach ($align in $alignments) {
            if ($align -eq 0) { continue }
            $isPowerOfTwo = (($align -band ($align - 1)) -eq 0)
            if (-not $isPowerOfTwo -or $align -lt 16384) {
                $isBad = $true
                break
            }
        }
        if ($isBad) {
            $badLine = "{0} (PT_LOAD alignments: {1})" -f $entry.FullName, ($alignments -join ",")
            [void]$bad.Add($badLine)
        }
    }

    $name = if ([string]::IsNullOrWhiteSpace($Label)) { $resolved.Path } else { $Label }
    Write-Host "16KB verification target: $name"
    Write-Host "Native ELF files checked: $checked"

    if ($bad.Count -gt 0) {
        Write-Host ""
        Write-Host "FAILED: Non-16KB-compatible native libraries found:"
        foreach ($line in $bad) {
            Write-Host " - $line"
        }
        exit 1
    }

    Write-Host "PASS: All inspected native libraries are 16KB-compatible."
    exit 0
} finally {
    $zip.Dispose()
}
