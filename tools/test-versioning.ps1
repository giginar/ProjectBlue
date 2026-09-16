[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$wrapper = Join-Path $projectRoot $(if ($env:OS -eq 'Windows_NT') { 'gradlew.bat' } else { 'gradlew' })
$fixtureRoot = Join-Path $projectRoot ('build/versioning-tests/' + [guid]::NewGuid().ToString('N'))
$fixture = Join-Path $fixtureRoot 'repo'
$utf8 = [System.Text.UTF8Encoding]::new($false)
New-Item -ItemType Directory -Path $fixture -Force | Out-Null

function Write-Fixture([string]$relative, [string]$contents) {
    [System.IO.File]::WriteAllText((Join-Path $fixture $relative), $contents, $utf8)
}
function Invoke-FixtureGit([string[]]$GitArguments) {
    & git -C $fixture @GitArguments
    if ($LASTEXITCODE -ne 0) { throw "Fixture git command failed: $GitArguments" }
}
function Assert-Equal($actual, $expected, [string]$label) {
    if ($actual -cne $expected) { throw "$label : expected '$expected', got '$actual'" }
}
function Read-Version([string]$directory, [string]$expectedError = '') {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & $wrapper '-p' $directory '-q' ':writeVersionInfo' '--console=plain' 2>&1
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    if ($expectedError) {
        if ($code -eq 0 -or ($output | Out-String) -notmatch [regex]::Escape($expectedError)) {
            throw "Expected rejected build containing '$expectedError'. Output: $output"
        }
        return
    }
    if ($code -ne 0) { throw "Version fixture build failed: $output" }
    return Get-Content -LiteralPath (Join-Path $directory 'build/version/version.json') -Raw -Encoding UTF8 | ConvertFrom-Json
}

Write-Fixture 'settings.gradle' "rootProject.name = 'versioning-fixture'`n"
Write-Fixture 'build.gradle' "plugins { id 'base' }`napply from: 'versioning.gradle'`n"
Write-Fixture '.gitignore' ".gradle/`nbuild/`n"
Write-Fixture 'probe.txt' "original`n"
Copy-Item -LiteralPath (Join-Path $projectRoot 'gradle/versioning.gradle') -Destination (Join-Path $fixture 'versioning.gradle')
Copy-Item -LiteralPath (Join-Path $projectRoot 'version.properties') -Destination (Join-Path $fixture 'version.properties')

Read-Version $fixture 'root of its own Git checkout'
Invoke-FixtureGit @('init', '-q', '-b', 'main')
Invoke-FixtureGit @('config', 'user.name', 'Project Blue Version Test')
Invoke-FixtureGit @('config', 'user.email', 'version-test@example.invalid')
Invoke-FixtureGit @('config', 'commit.gpgsign', 'false')
Invoke-FixtureGit @('config', 'core.autocrlf', 'false')
Read-Version $fixture 'at least one commit'
Invoke-FixtureGit @('add', '.')
Invoke-FixtureGit @('commit', '-q', '-m', 'First fixture commit')

$first = Read-Version $fixture
Assert-Equal $first.versionCode 1 'Initial Android versionCode'
Assert-Equal $first.dirty $false 'Clean checkout'
$again = Read-Version $fixture
Assert-Equal $again.versionName $first.versionName 'Repeat builds use the same version'
Assert-Equal $again.gitCommit $first.gitCommit 'Repeat builds use the same commit'
Assert-Equal $first.apkFileName ("ProjectBlue-" + $first.versionName + "-debug.apk") 'APK uses the same version'

Write-Fixture 'probe.txt' "edited`n"
$dirty = Read-Version $fixture
Assert-Equal $dirty.dirty $true 'Tracked changes are marked dirty'
Assert-Equal $dirty.versionName ($first.versionName + '-dirty') 'Dirty suffix'
Assert-Equal $dirty.versionCode $first.versionCode 'Uncommitted edits do not invent a commit'
Invoke-FixtureGit @('add', 'probe.txt')
Invoke-FixtureGit @('commit', '-q', '-m', 'Second fixture commit')
$second = Read-Version $fixture
Assert-Equal $second.versionCode 2 'Each commit increments Android versionCode'
Assert-Equal $second.dirty $false 'Committed changes are clean'
if ($second.versionName -ceq $first.versionName) { throw 'Different commits reused a version.' }

Write-Fixture 'untracked.txt' "new source`n"
$untracked = Read-Version $fixture
Assert-Equal $untracked.dirty $true 'Untracked source is marked dirty'
# Keep the file ignored only for the following branch test; no user files are touched.
[System.IO.File]::AppendAllText((Join-Path $fixture '.git/info/exclude'), "`nuntracked.txt`n", $utf8)
Invoke-FixtureGit @('checkout', '-q', '-b', 'alternate', $first.gitCommit)
Write-Fixture 'probe.txt' "alternate branch`n"
Invoke-FixtureGit @('add', 'probe.txt')
Invoke-FixtureGit @('commit', '-q', '-m', 'Alternative second commit')
$alternate = Read-Version $fixture
Assert-Equal $alternate.versionCode $second.versionCode 'Equal branch heights have equal codes'
if ($alternate.versionName -ceq $second.versionName) { throw 'Branch commits with equal counts need distinct version names.' }

$shallow = Join-Path $fixtureRoot 'shallow'
& git clone --quiet --no-local --depth 1 $fixture $shallow
if ($LASTEXITCODE -ne 0) { throw 'Could not create shallow fixture.' }
Read-Version $shallow 'complete Git history'
Write-Host 'PASS: clean/repeated builds, dirty/untracked files, commit increments, branch uniqueness, unborn/nested/shallow rejection.'
Write-Host "Fixtures: $fixtureRoot"
# GitHub's pwsh runner propagates the last native exit code, including expected rejections.
exit 0
