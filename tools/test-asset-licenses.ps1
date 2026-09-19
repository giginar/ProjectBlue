[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$probe = Join-Path $projectRoot ('assets/licenses/qa-unlisted-' + [guid]::NewGuid().ToString('N') + '.txt')
$created = $false
Push-Location $projectRoot
try {
    # This single new file checks that the inventory directory cannot bypass packaging review.
    if (Test-Path -LiteralPath $probe) { throw 'Asset audit probe path already exists.' }
    [IO.File]::WriteAllText($probe, 'Original temporary QA probe, not a game asset.')
    $created = $true
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & .\gradlew.bat :verifyAssetLicenses --console=plain 2>&1
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    if ($code -eq 0 -or ($output | Out-String) -notmatch 'Asset allowlist mismatch') {
        throw 'Unlisted content under assets/licenses did not fail the packaging gate as expected.'
    }
    Write-Host 'Asset license regression passed: unlisted inventory-directory content was rejected.'
} finally {
    if ($created) { Remove-Item -LiteralPath $probe -Force }
    Pop-Location
}
