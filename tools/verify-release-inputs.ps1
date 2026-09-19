[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    # Inspect tracked and non-ignored files; never print matched secrets or IDs.
    $paths = @(& git -c core.quotepath=false ls-files --cached --others --exclude-standard) | Select-Object -Unique
    if ($LASTEXITCODE -ne 0) { throw 'Cannot enumerate repository files.' }
    $forbiddenPath = '(^|/)(local\.properties|keystore\.properties|signing\.properties|ads\.properties|secrets\.properties|\.env(\..*)?)$|\.(jks|keystore|p12|pem|key)$|(^|/)secrets/'
    $officialIds = @('ca-app-pub-3940256099942544~3347511713', 'ca-app-pub-3940256099942544/5224354917', 'ca-app-pub-3940256099942544/1033173712')
    foreach ($path in $paths) {
        if ($path -match $forbiddenPath) { throw "Sensitive file is not ignored: $path" }
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { continue }
        if ($path -notmatch '\.(java|gradle|properties|xml|md|ps1|yml|yaml|json|txt)$') { continue }
        $contents = [IO.File]::ReadAllText((Join-Path $projectRoot $path))
        foreach ($match in [regex]::Matches($contents, 'ca-app-pub-\d{16}[~/]\d{10}')) {
            if ($match.Value -notin $officialIds) { throw "Non-test ad identifier found in: $path" }
        }
        if ($contents -match '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----') { throw "Private key found in: $path" }
        if ($contents -match '(?im)^\s*(storePassword|keyPassword)\s*[=:]\s*["''][^"'']+["'']') {
            throw "Literal signing password found in: $path"
        }
    }
    $staged = & git diff --cached --no-ext-diff --unified=0
    foreach ($match in [regex]::Matches(($staged -join "`n"), 'ca-app-pub-\d{16}[~/]\d{10}')) {
        if ($match.Value -notin $officialIds) { throw 'Non-test ad identifier found in the Git index.' }
    }
    foreach ($ignored in @('local.properties', 'signing.properties', 'ads.properties', 'secrets.properties', 'upload.jks', 'upload.keystore', '.env', 'secrets/example.json')) {
        & git check-ignore -q -- $ignored
        if ($LASTEXITCODE -ne 0) { throw "Missing ignore rule: $ignored" }
    }
    Write-Host 'Release input checks passed: only official test ad IDs; sensitive paths ignored; no literal signing credentials.'
} finally { Pop-Location }
