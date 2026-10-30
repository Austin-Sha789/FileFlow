# Prepare the pinned video engine once after cloning. No runtime downloads.
$ErrorActionPreference = 'Stop'
$tag = 'autobuild-2026-10-29-19-17'
$asset = 'ffmpeg-n9.0.2-37-gae19868753-win64-lgpl-shared-9.0.zip'
$expectedHash = '44280849ca9ad4cffa466e1f244b8eaf3c4f28c3b1bef00801fadba9ec4f676b'
$project = Split-Path -Parent $PSScriptRoot
$destination = Join-Path $project 'tools/ffmpeg'
$temporary = Join-Path ([System.IO.Path]::GetTempPath()) ('fileflow-ffmpeg-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temporary -Force | Out-Null
$download = Join-Path $temporary 'download.zip'
$bundlePath = Join-Path $temporary 'windows-x86_64.zip'
try {
    Invoke-WebRequest -Uri "https://github.com/BtbN/FFmpeg-Builds/releases/download/$tag/$asset" -OutFile $download
    if ((Get-FileHash -LiteralPath $download -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {
        throw 'FFmpeg download checksum mismatch.'
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $source = [System.IO.Compression.ZipFile]::OpenRead($download)
    $bundle = [System.IO.Compression.ZipFile]::Open($bundlePath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($entry in $source.Entries) {
            if (($entry.FullName.Contains('/bin/') -and ($entry.Name.EndsWith('.dll') -or $entry.Name -eq 'ffmpeg.exe')) -or $entry.Name -eq 'LICENSE.txt') {
                $target = $bundle.CreateEntry($entry.Name, [System.IO.Compression.CompressionLevel]::Optimal)
                $input = $entry.Open()
                $output = $target.Open()
                try { $input.CopyTo($output) } finally { $output.Dispose(); $input.Dispose() }
            }
        }
    } finally { $bundle.Dispose(); $source.Dispose() }
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    Copy-Item -LiteralPath $bundlePath -Destination (Join-Path $destination 'windows-x86_64.zip') -Force
    Write-Host 'Video engine prepared. Maven includes it in the application resources.'
} finally {
    # Delete only the two known files and the empty directory created by this invocation.
    if ([System.IO.File]::Exists($download)) { [System.IO.File]::Delete($download) }
    if ([System.IO.File]::Exists($bundlePath)) { [System.IO.File]::Delete($bundlePath) }
    [System.IO.Directory]::Delete($temporary)
}