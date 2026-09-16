[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('setup', 'prepare', 'build', 'verify', 'install', 'devices', 'logs', 'version')]
    [string]$Command = 'build',
    [string]$Serial,
    [string]$SdkPath
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$toolsVersion = '15859902'
$toolsSha256 = '90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a'
$localProperties = Join-Path $projectRoot 'local.properties'
$utf8 = [System.Text.UTF8Encoding]::new($false)

function Get-SdkRoot {
    if ($SdkPath) { return [System.IO.Path]::GetFullPath($SdkPath) }
    if (Test-Path -LiteralPath $localProperties) {
        $line = Get-Content -LiteralPath $localProperties -Encoding UTF8 |
            Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -Last 1
        if ($line) {
            return [System.IO.Path]::GetFullPath(($line -replace '^\s*sdk\.dir\s*=\s*', '').Replace('\:', ':').Replace('\\', '\'))
        }
    }
    if ($env:ANDROID_HOME) { return [System.IO.Path]::GetFullPath($env:ANDROID_HOME) }
    if ($env:ANDROID_SDK_ROOT) { return [System.IO.Path]::GetFullPath($env:ANDROID_SDK_ROOT) }
    return Join-Path $env:LOCALAPPDATA 'Android\Sdk'
}
$sdkRoot = Get-SdkRoot

function Set-JavaEnvironment {
    if (-not $env:JAVA_HOME) {
        $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
        if (-not $compiler) { throw 'JDK 17 or newer is required. Set JAVA_HOME or add the JDK bin directory to PATH.' }
        $env:JAVA_HOME = Split-Path (Split-Path $compiler.Source -Parent) -Parent
    }
    if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
        throw "JAVA_HOME must point to a JDK: $env:JAVA_HOME"
    }
    $env:ANDROID_HOME = $sdkRoot
    $env:ANDROID_SDK_ROOT = $sdkRoot
}

function Save-SdkLocation {
    $lines = @()
    if (Test-Path -LiteralPath $localProperties) {
        $lines = @(Get-Content -LiteralPath $localProperties -Encoding UTF8 |
            Where-Object { $_ -notmatch '^\s*sdk\.dir\s*=' })
    }
    $lines += 'sdk.dir=' + $sdkRoot.Replace('\', '/').Replace(':', '\:')
    [System.IO.File]::WriteAllText($localProperties, ($lines -join "`n") + "`n", $utf8)
}

function Install-AndroidSdk {
    Set-JavaEnvironment
    $manager = Join-Path $sdkRoot "cmdline-tools\$toolsVersion\bin\sdkmanager.bat"
    if (-not (Test-Path -LiteralPath $manager)) {
        # Official Google archive and published SHA-256, checked on 2026-09-16.
        $cache = Join-Path $env:TEMP 'projectblue-android-tools'
        New-Item -ItemType Directory -Path $cache -Force | Out-Null
        $archive = Join-Path $cache "commandlinetools-win-${toolsVersion}_latest.zip"
        if (-not (Test-Path -LiteralPath $archive)) {
            Write-Host 'Downloading Android command-line tools from Google...'
            $oldProgress = $ProgressPreference
            try {
                $ProgressPreference = 'SilentlyContinue'
                Invoke-WebRequest "https://dl.google.com/android/repository/commandlinetools-win-${toolsVersion}_latest.zip" -OutFile $archive -UseBasicParsing
            } finally { $ProgressPreference = $oldProgress }
        }
        if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $toolsSha256) {
            throw "SDK archive checksum mismatch. Remove this archive and retry: $archive"
        }
        $staging = Join-Path $cache ('unpack-' + [guid]::NewGuid().ToString('N'))
        Expand-Archive -LiteralPath $archive -DestinationPath $staging
        try {
            $destination = Join-Path $sdkRoot "cmdline-tools\$toolsVersion"
            New-Item -ItemType Directory -Path $destination -Force | Out-Null
            Get-ChildItem -LiteralPath (Join-Path $staging 'cmdline-tools') |
                Copy-Item -Destination $destination -Recurse -Force
        } finally {
            $resolvedCache = (Resolve-Path -LiteralPath $cache).Path.TrimEnd('\') + '\'
            $resolvedStaging = (Resolve-Path -LiteralPath $staging).Path
            if (-not $resolvedStaging.StartsWith($resolvedCache, [StringComparison]::OrdinalIgnoreCase)) {
                throw 'Unsafe SDK temporary-directory cleanup path.'
            }
            Remove-Item -LiteralPath $resolvedStaging -Recurse -Force
        }
    }
    Write-Host 'Installing SDK 36, build-tools 35.0.0 and platform-tools.'
    Write-Host 'The required Android SDK package licenses are accepted through sdkmanager.'
    Write-Host 'License: https://developer.android.com/studio/terms'
    $answers = 1..20 | ForEach-Object { 'y' }
    $answers | & $manager "--sdk_root=$sdkRoot" '--install' 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
    if ($LASTEXITCODE -ne 0) { throw "sdkmanager failed with exit code $LASTEXITCODE" }
    Save-SdkLocation
    Write-Host "Android SDK ready: $sdkRoot"
}

function Ensure-AndroidSdk {
    Set-JavaEnvironment
    $required = @('platforms\android-36\android.jar', 'build-tools\35.0.0\apksigner.bat', 'platform-tools\adb.exe')
    foreach ($relative in $required) {
        if (-not (Test-Path -LiteralPath (Join-Path $sdkRoot $relative))) {
            Install-AndroidSdk
            return
        }
    }
    Save-SdkLocation
}

function Build-Apk {
    Ensure-AndroidSdk
    Push-Location $projectRoot
    try {
        & '.\gradlew.bat' ':check' ':android:lintDebug' ':android:packageDebugApk' '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }
        Test-Apk
    } finally { Pop-Location }
}

function Test-Apk {
    $version = Get-Content -LiteralPath (Join-Path $projectRoot 'build\version\version.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    $artifactDir = Join-Path (Join-Path $projectRoot 'build\artifacts') $version.versionName
    $script:apk = Join-Path $artifactDir $version.apkFileName
    if (-not (Test-Path -LiteralPath $script:apk)) { throw "APK missing: $script:apk" }
    & (Join-Path $sdkRoot 'build-tools\35.0.0\apksigner.bat') verify $script:apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    & (Join-Path $sdkRoot 'build-tools\35.0.0\zipalign.exe') -c -P 16 4 $script:apk
    if ($LASTEXITCODE -ne 0) { throw 'APK 16 KB alignment verification failed.' }
    Write-Host "Version: $($version.versionName) (Android code $($version.versionCode))"
    Write-Host "APK ready: $script:apk"
}

function Get-Device {
    $listing = & $script:adb devices
    if ($LASTEXITCODE -ne 0) { throw 'adb could not enumerate devices.' }
    $online = @($listing | Where-Object { $_ -match '^\S+\s+device$' } |
        ForEach-Object { ($_ -split '\s+')[0] })
    if ($Serial) {
        if ($online -notcontains $Serial) { throw "Device $Serial is not online. Run android.bat devices and authorize USB debugging on the phone." }
        return $Serial
    }
    if ($online.Count -eq 0) { throw 'No authorized Android device. Enable USB debugging, connect a data cable, and accept the phone prompt. Run android.bat devices.' }
    if ($online.Count -gt 1) { throw 'Multiple devices found. Choose one with -Serial DEVICE_ID.' }
    return $online[0]
}

try {
    if ($Command -eq 'version') {
        Set-JavaEnvironment
        Push-Location $projectRoot
        try {
            & '.\gradlew.bat' '-q' ':printVersion'
            if ($LASTEXITCODE -ne 0) { throw 'Could not determine the Git version.' }
        } finally { Pop-Location }
        exit 0
    }
    if ($Command -eq 'setup') { Install-AndroidSdk; exit 0 }
    if ($Command -in @('build', 'install')) { Build-Apk }
    else { Ensure-AndroidSdk }
    $script:adb = Join-Path $sdkRoot 'platform-tools\adb.exe'
    switch ($Command) {
        'prepare' { Write-Host "Android SDK ready: $sdkRoot" }
        'verify' { Test-Apk }
        'devices' {
            & $script:adb devices -l
            if ($LASTEXITCODE -ne 0) { throw 'adb devices failed.' }
        }
        'install' {
            $device = Get-Device
            & $script:adb -s $device install -r $script:apk
            if ($LASTEXITCODE -ne 0) { throw 'APK install failed. If signatures differ, manually uninstall the old app first (this removes its profile).' }
            & $script:adb -s $device shell am start -W -n 'com.projectblue.game/com.projectblue.game.android.AndroidLauncher'
            if ($LASTEXITCODE -ne 0) { throw 'Android app launch failed.' }
        }
        'logs' {
            $device = Get-Device
            & $script:adb -s $device logcat 'AndroidRuntime:E' 'ProjectBlue:V' 'libgdx:V' '*:S'
            if ($LASTEXITCODE -ne 0) { throw 'adb logcat failed.' }
        }
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
