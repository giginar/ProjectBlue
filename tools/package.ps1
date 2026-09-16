[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('All', 'Windows', 'Android')]
    [string]$Target = 'All',
    [switch]$OpenOutput,
    [switch]$SmokeTest
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$buildRoot = Join-Path $projectRoot 'build\packaging'
$distRoot = Join-Path $projectRoot 'dist'
$utf8 = [System.Text.UTF8Encoding]::new($false)
$withWindows = $Target -in @('All', 'Windows')
$withAndroid = $Target -in @('All', 'Android')
$lock = $null
$staging = $null
$oldPath = $env:PATH

function Write-Utf8([string]$Path, [string]$Content) {
    [System.IO.File]::WriteAllText($Path, $Content, $utf8)
}

function Assert-ChildPath([string]$Path, [string]$Parent) {
    # Resolve actual existing paths before any recursive move or deletion.
    $resolved = (Resolve-Path -LiteralPath $Path).Path
    $prefix = (Resolve-Path -LiteralPath $Parent).Path.TrimEnd('\') + '\'
    if (-not $resolved.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path is outside the packaging workspace: $resolved"
    }
    # Do not follow junctions/symlinks in packaging output trees.
    $items = @(Get-Item -LiteralPath $resolved) + @(Get-ChildItem -LiteralPath $resolved -Force -Recurse)
    if ($items | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }) {
        throw "Packaging path contains a reparse point: $resolved"
    }
}

function Assert-MoveDestination([string]$Path, [string]$Parent) {
    $prefix = (Resolve-Path -LiteralPath $Parent).Path.TrimEnd('\') + '\'
    $fullPath = [IO.Path]::GetFullPath($Path)
    $resolvedParent = (Resolve-Path -LiteralPath (Split-Path $fullPath -Parent)).Path.TrimEnd('\') + '\'
    if (-not $fullPath.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase) -or
        -not $resolvedParent.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Move destination is outside the packaging workspace: $fullPath"
    }
}

function Set-PackagingJava {
    if (-not $env:JAVA_HOME) {
        $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
        if (-not $compiler) { throw 'Install a JDK 17 or 21 and set JAVA_HOME.' }
        $env:JAVA_HOME = Split-Path (Split-Path $compiler.Source -Parent) -Parent
    }
    if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
        throw "JAVA_HOME must point to a JDK: $env:JAVA_HOME"
    }
    if ($withWindows) {
        $script:jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
        if (-not (Test-Path -LiteralPath $script:jpackage)) { throw 'Windows packaging requires a JDK with jpackage.' }
        $script:javaVersion = (& $script:jpackage --version | Out-String).Trim()
        if ($LASTEXITCODE -ne 0 -or $script:javaVersion -notmatch '^(17|21)\.') {
            throw 'Use JDK 17 or 21 for this verified jpackage/WiX 3 toolchain.'
        }
        $release = Get-Content -LiteralPath (Join-Path $env:JAVA_HOME 'release') -Raw
        if ($release -notmatch 'OS_ARCH="(amd64|x86_64)"') { throw 'Windows packages require an x64 JDK.' }
    }
}

function Ensure-Wix {
    # Pinned official WiX release, hash measured from this exact archive on 2026-09-16.
    # Portable build tools only: no system installation, account or machine PATH change.
    $cache = Join-Path $env:LOCALAPPDATA 'ProjectBlue\build-tools'
    $archive = Join-Path $cache 'wix314-binaries.zip'
    $expected = '6ac824e1642d6f7277d0ed7ea09411a508f6116ba6fae0aa5f2c7daa2ff43d31'
    New-Item -ItemType Directory -Path $cache -Force | Out-Null
    if (-not (Test-Path -LiteralPath $archive)) {
        Write-Host 'Downloading portable WiX 3.14.1 build tools...'
        $download = Join-Path $cache ('download-' + [guid]::NewGuid().ToString('N') + '.zip')
        $oldProgress = $ProgressPreference
        try {
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest 'https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip' -OutFile $download -UseBasicParsing
            if ((Get-FileHash -LiteralPath $download -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) {
                throw "WiX checksum mismatch: $download"
            }
            Move-Item -LiteralPath $download -Destination $archive
        } finally { $ProgressPreference = $oldProgress }
    }
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) {
        throw "WiX checksum mismatch. Remove this archive and retry: $archive"
    }
    $wixDir = Join-Path $cache 'wix-3.14.1'
    $marker = Join-Path $wixDir '.ready.sha256'
    if (-not (Test-Path -LiteralPath $marker) -or (Get-Content -LiteralPath $marker -Raw).Trim() -ne $expected) {
        Expand-Archive -LiteralPath $archive -DestinationPath $wixDir -Force
        Write-Utf8 $marker $expected
    }
    foreach ($tool in @('candle.exe', 'light.exe')) {
        if (-not (Test-Path -LiteralPath (Join-Path $wixDir $tool))) { throw "Incomplete WiX cache: $wixDir" }
    }
    $env:PATH = $wixDir + ';' + $env:PATH
}

function Test-WindowsImage([string]$ImagePath) {
    $work = Join-Path $staging 'smoke'
    New-Item -ItemType Directory -Path $work | Out-Null
    $stdout = Join-Path $work 'stdout.log'
    $stderr = Join-Path $work 'stderr.log'
    $savedJava = $env:JAVA_HOME
    $savedPath = $env:PATH
    try {
        # The native launcher must work using ONLY its bundled runtime.
        $env:JAVA_HOME = ''
        $env:PATH = "$env:SystemRoot\System32;$env:SystemRoot"
        $process = Start-Process -FilePath (Join-Path $ImagePath 'ProjectBlue.exe') -ArgumentList '--smoke' -WorkingDirectory $work -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
        $null = $process.Handle # Retain the native handle so Windows PowerShell reports ExitCode after exit.
        if (-not $process.WaitForExit(60000)) {
            $process.Kill()
            throw "Packaged game smoke test timed out. Logs: $work"
        }
        $process.WaitForExit()
        $log = Get-Content -LiteralPath $stdout -Raw
        if ($process.ExitCode -ne 0 -or $log -notmatch 'SMOKE.*PASS:') {
            throw "Packaged game smoke test failed (exit $($process.ExitCode)). Logs: $work"
        }
        Write-Host 'PASS: packaged EXE, bundled Java, gameplay, pause, lifecycle, results.'
    } finally {
        $env:JAVA_HOME = $savedJava
        $env:PATH = $savedPath
    }
}

try {
    if ($env:OS -ne 'Windows_NT') { throw 'Run paketle.bat on Windows.' }
    if ($SmokeTest -and -not $withWindows) { throw '-SmokeTest requires Windows or All.' }
    New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
    try {
        $lock = [IO.File]::Open((Join-Path $buildRoot '.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    } catch { throw 'Another local packaging process is running. Wait for it to finish.' }
    Set-PackagingJava
    if ($withWindows) { Ensure-Wix }
    if ($withAndroid) {
        & (Join-Path $projectRoot 'android.bat') prepare
        if ($LASTEXITCODE -ne 0) { throw 'Android SDK preparation failed.' }
    }

    # One Gradle invocation gives both platforms exactly the same version snapshot.
    $gradleTasks = @(':check', ':writeVersionInfo')
    if ($withWindows) { $gradleTasks += ':lwjgl3:prepareWindowsPackage' }
    if ($withAndroid) { $gradleTasks += @(':android:lintDebug', ':android:packageDebugApk') }
    Push-Location $projectRoot
    try {
        & '.\gradlew.bat' @gradleTasks '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw 'Gradle checks/build failed. No new distribution was published.' }
    } finally { Pop-Location }
    $version = Get-Content -LiteralPath (Join-Path $projectRoot 'build\version\version.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($version.versionName -notmatch '^\d+\.\d+\.\d+-g[0-9a-f]{12}(-dirty)?$') { throw 'Invalid build version.' }
    $staging = Join-Path $buildRoot ([guid]::NewGuid().ToString('N'))
    $output = Join-Path $staging 'output'
    New-Item -ItemType Directory -Path $output -Force | Out-Null
    $prefix = 'ProjectBlue-' + $version.versionName
    $artifacts = @()
    $windowsVersion = $null

    if ($withAndroid) {
        & (Join-Path $projectRoot 'android.bat') verify
        if ($LASTEXITCODE -ne 0) { throw 'APK verification failed.' }
        $apk = Join-Path (Join-Path (Join-Path $projectRoot 'build\artifacts') $version.versionName) $version.apkFileName
        $name = "$prefix-android.apk"
        Copy-Item -LiteralPath $apk -Destination (Join-Path $output $name)
        $artifacts += $name
    }
    if ($withWindows) {
        # MSI accepts numeric major.minor.build only; filenames/game retain the full Git version.
        $windowsVersion = $version.versionName.Split('-')[0]
        $numeric = [version]$windowsVersion
        if ($numeric.Major -gt 255 -or $numeric.Minor -gt 255 -or $numeric.Build -gt 65535) {
            throw 'Windows Installer version limits exceeded: major/minor <= 255, commit count <= 65535.'
        }
        $icon = Join-Path $staging 'ProjectBlue.ico'
        & (Join-Path $env:JAVA_HOME 'bin\java.exe') (Join-Path $PSScriptRoot 'GenerateWindowsIcon.java') $icon
        if ($LASTEXITCODE -ne 0) { throw 'Windows icon generation failed.' }
        $images = Join-Path $staging 'images'
        $imagePath = Join-Path $images 'ProjectBlue'
        Write-Host "Packaging Windows x64 with Java $script:javaVersion..."
        & $script:jpackage --type app-image --name ProjectBlue --app-version $windowsVersion --vendor 'Project Blue' --description "Project Blue $($version.versionName)" --input (Join-Path $projectRoot 'lwjgl3\build\windows-package-input') --main-jar "lwjgl3-$($version.versionName).jar" --main-class 'com.projectblue.game.lwjgl3.DesktopLauncher' --dest $images --icon $icon --add-modules 'java.base,java.desktop,java.logging,jdk.unsupported' --java-options '-Xmx256m'
        if ($LASTEXITCODE -ne 0) { throw 'jpackage app-image failed.' }
        Copy-Item -LiteralPath (Join-Path $projectRoot 'build\version\version.json') -Destination (Join-Path $imagePath 'BUILD.json')
        Copy-Item -LiteralPath (Join-Path $projectRoot 'assets\licenses') -Destination (Join-Path $imagePath 'asset-licenses') -Recurse
        Write-Utf8 (Join-Path $imagePath 'PLAY.txt') "Project Blue $($version.versionName)`r`nStart ProjectBlue.exe. Java is included in runtime/. Keep the entire folder together.`r`nDrag with the left mouse button. Esc / P pauses. Profile: %USERPROFILE%\.projectblue`r`n"
        if ($SmokeTest) { Test-WindowsImage $imagePath }

        $installers = Join-Path $staging 'installers'
        # Stable upgrade identity: newer commits update the same per-user application.
        & $script:jpackage --type exe --name ProjectBlue --app-version $windowsVersion --vendor 'Project Blue' --description "Project Blue $($version.versionName)" --app-image $imagePath --dest $installers --win-per-user-install --install-dir ProjectBlue --win-menu --win-menu-group 'Project Blue' --win-shortcut --win-dir-chooser --win-upgrade-uuid 'cbeb7e09-6b3a-47ee-8134-a86eaa6ad6f2'
        if ($LASTEXITCODE -ne 0) { throw 'jpackage Windows installer failed.' }
        $built = @(Get-ChildItem -LiteralPath $installers -Filter '*.exe' -File)
        if ($built.Count -ne 1) { throw 'Expected exactly one Windows installer.' }
        $name = "$prefix-windows-x64-setup.exe"
        Copy-Item -LiteralPath $built[0].FullName -Destination (Join-Path $output $name)
        $artifacts += $name
        $name = "$prefix-windows-x64-portable.zip"
        # .NET includes every runtime/legal file, including files Compress-Archive may skip.
        Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
        $zip = [IO.Compression.ZipFile]::Open((Join-Path $output $name), [IO.Compression.ZipArchiveMode]::Create)
        try {
            Get-ChildItem -LiteralPath $imagePath -File -Recurse -Force | ForEach-Object {
                # Windows PowerShell's ZipFile otherwise uses backslashes, contrary to the ZIP format.
                $entry = 'ProjectBlue/' + $_.FullName.Substring($imagePath.Length + 1).Replace('\', '/')
                [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $entry, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
            }
        } finally { $zip.Dispose() }
        $artifacts += $name
    }

    $files = @($artifacts | ForEach-Object {
        $path = Join-Path $output $_
        [ordered]@{ name = $_; bytes = (Get-Item -LiteralPath $path).Length; sha256 = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() }
    })
    $metadata = [ordered]@{
        versionName = $version.versionName; versionCode = $version.versionCode
        gitCommit = $version.gitCommit; dirty = $version.dirty; target = $Target
        windowsInstallerVersion = $windowsVersion
        bundledJavaVersion = $(if ($withWindows) { $script:javaVersion } else { $null })
        windowsSmokeTest = [bool]$SmokeTest; artifacts = $files
    }
    Write-Utf8 (Join-Path $output 'BUILD.json') (($metadata | ConvertTo-Json -Depth 5) + "`n")
    Write-Utf8 (Join-Path $output 'SHA256SUMS.txt') (($files | ForEach-Object { "$($_.sha256)  $($_.name)" }) -join "`n")
    Write-Utf8 (Join-Path $output 'OKU.txt') @"
Project Blue $($version.versionName)

Android: *-android.apk dosyasini telefona gonder ve ac. Android 8.0+ gerekir.
Telefon isterse dosyayi actigin uygulamaya APK yukleme izni ver.
Windows: *-windows-x64-setup.exe dosyasini calistir. Java pakete dahildir.
Kurulumsuz alternatif: *-portable.zip dosyasini TAMAMEN cikart, ProjectBlue.exe'yi ac.
Windows paketi x64 icindir. APK debug imzalidir; Windows EXE kod imzasizdir.

Sol mouse tusuyla / parmaginla surukle. Esc / P / pause dugmesi ile duraklat.
180 saniye hayatta kal, plastikleri temizle ve kaplumbagalari kurtar.
Windows kayitlari: %USERPROFILE%\.projectblue

Dosya adindaki surum, oyun menusundeki surumle aynidir.
Yeni commit yeni surumdur; -dirty commitlenmemis degisiklik demektir.
BUILD.json dosya boyutlarini ve SHA-256 dogrulama degerlerini icerir.
"@

    # Publish only a complete, verified set. Failed builds leave the previous output intact.
    New-Item -ItemType Directory -Path $distRoot -Force | Out-Null
    $destination = Join-Path $distRoot $version.versionName
    $backup = $null
    Assert-ChildPath $output $buildRoot
    Assert-MoveDestination $destination $distRoot
    if (Test-Path -LiteralPath $destination) {
        Assert-ChildPath $destination $distRoot
        $backup = Join-Path $buildRoot ('previous-' + [guid]::NewGuid().ToString('N'))
        Assert-MoveDestination $backup $buildRoot
        Move-Item -LiteralPath $destination -Destination $backup
    }
    try { Move-Item -LiteralPath $output -Destination $destination }
    catch {
        if ($backup) {
            Assert-ChildPath $backup $buildRoot
            Move-Item -LiteralPath $backup -Destination $destination
        }
        throw
    }
    Write-Utf8 (Join-Path $distRoot 'LATEST.txt') ($version.versionName + "`n")
    Write-Host "`nREADY: $destination" -ForegroundColor Green
    foreach ($artifact in $files) { Write-Host ("  {0} ({1:N1} MB)" -f $artifact.name, ($artifact.bytes / 1MB)) }
    if ($version.dirty) { Write-Host 'Uncommitted source: -dirty suffix. Commit changes for a clean version.' }
    Assert-ChildPath $staging $buildRoot
    Remove-Item -LiteralPath $staging -Recurse -Force
    if ($OpenOutput) { Start-Process explorer.exe -ArgumentList ('"' + $destination + '"') }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    if ($staging) { Write-Host "Packaging work/logs: $staging" }
    exit 1
} finally {
    $env:PATH = $oldPath
    if ($lock) { $lock.Dispose() }
}
exit 0
