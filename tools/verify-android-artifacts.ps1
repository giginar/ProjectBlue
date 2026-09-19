[CmdletBinding()]
param([string]$SdkPath = "$env:LOCALAPPDATA/Android/Sdk")
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Add-Type -AssemblyName System.IO.Compression.FileSystem
$apk = Join-Path $projectRoot 'android/build/outputs/apk/debug/android-debug.apk'
$bundle = Join-Path $projectRoot 'android/build/outputs/bundle/release/android-release.aab'
& (Join-Path $SdkPath 'build-tools/35.0.0/apksigner.bat') verify $apk
if ($LASTEXITCODE -ne 0) { throw 'Debug APK signature verification failed.' }
& (Join-Path $SdkPath 'build-tools/35.0.0/zipalign.exe') -c -P 16 4 $apk
if ($LASTEXITCODE -ne 0) { throw 'Debug APK ZIP alignment verification failed.' }
foreach ($archivePath in @($apk, $bundle)) {
    $archive = [IO.Compression.ZipFile]::OpenRead($archivePath)
    try {
        $libraries = @($archive.Entries | Where-Object { $_.FullName -match 'lib/(arm64-v8a|x86_64)/[^/]+\.so$' })
        if ($libraries.Count -lt 2) { throw 'Missing 64-bit native libraries.' }
        foreach ($entry in $libraries) {
            $stream = $entry.Open()
            $buffer = New-Object IO.MemoryStream
            try { $stream.CopyTo($buffer); $bytes = $buffer.ToArray() }
            finally { $stream.Dispose(); $buffer.Dispose() }
            if ($bytes[0] -ne 127 -or $bytes[1] -ne 69 -or $bytes[2] -ne 76 -or $bytes[3] -ne 70 -or $bytes[4] -ne 2 -or $bytes[5] -ne 1) {
                throw "Expected little-endian ELF64: $($entry.FullName)"
            }
            $headers = [BitConverter]::ToUInt64($bytes, 32)
            $entrySize = [BitConverter]::ToUInt16($bytes, 54)
            $count = [BitConverter]::ToUInt16($bytes, 56)
            $loads = 0
            for ($index = 0; $index -lt $count; $index++) {
                $offset = [int]($headers + $index * $entrySize)
                if ([BitConverter]::ToUInt32($bytes, $offset) -ne 1) { continue }
                $loads++
                $alignment = [BitConverter]::ToUInt64($bytes, $offset + 48)
                $fileOffset = [BitConverter]::ToUInt64($bytes, $offset + 8)
                $virtualAddress = [BitConverter]::ToUInt64($bytes, $offset + 16)
                if ($alignment -lt 16384 -or $fileOffset % 16384 -ne $virtualAddress % 16384) {
                    throw "ELF LOAD segment is not 16 KB aligned: $($entry.FullName)"
                }
            }
            if ($loads -eq 0) { throw 'ELF has no LOAD segments.' }
            Write-Host "Verified ELF 16 KB alignment: $($entry.FullName)"
        }
        if ($archivePath -eq $bundle) {
            foreach ($required in @('BundleConfig.pb', 'base/manifest/AndroidManifest.xml', 'base/dex/classes.dex')) {
                if (-not $archive.GetEntry($required)) { throw "Bundle entry missing: $required" }
            }
            $signatures = @($archive.Entries | Where-Object { $_.FullName -match '^META-INF/.*\.(RSA|DSA|EC)$' })
            Write-Host "Release AAB signature blocks: $($signatures.Count) (zero means unsigned verification artifact)."
        }
    } finally { $archive.Dispose() }
}
Write-Host 'Android artifact structure, debug signature, ZIP and 64-bit ELF alignment checks passed.'
