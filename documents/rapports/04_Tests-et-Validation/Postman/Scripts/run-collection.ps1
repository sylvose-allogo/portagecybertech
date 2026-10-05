# ============================================================================
# Organization : Portage Cybertech
# Project      : Mini OAuth 2.0 platform
# Purpose      : Runs the selected Postman collection against a named local environment and writes machine-readable test results to the configured report location.
# Author       : Sylvose Allogo <sylvose.allogo@yahoo.com>
# Version      : 1.0.0
# Date         : 2026-10-04
# ============================================================================
param(
    [ValidateSet('Dev', 'Int', 'Prod', 'All')]
    [string] $EnvironmentName = 'All'
)

$ErrorActionPreference = 'Stop'

$postmanCommand = Get-Command 'postman' -ErrorAction SilentlyContinue
if (-not $postmanCommand) {
    throw "Postman CLI is not installed or not available on PATH. Install the official Postman CLI before running this script."
}

$collectionPath = Join-Path $PSScriptRoot 'Portage CyberTech - OAuth2 End-to-End.postman_collection.json'
$reportsRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
$testsDirectory = Join-Path $reportsRoot '04_Tests-et-Validation\Rapports-historiques\Postman'
$environmentNames = if ($EnvironmentName -eq 'All') {
    @('Dev', 'Int', 'Prod')
} else {
    @($EnvironmentName)
}
$runTimestamp = Get-Date -Format 'yyyyMMdd-HHmmss'

if (-not (Test-Path -LiteralPath $collectionPath -PathType Leaf)) {
    throw "Postman collection not found: $collectionPath"
}
if (-not (Test-Path -LiteralPath $testsDirectory -PathType Container)) {
    New-Item -ItemType Directory -Path $testsDirectory -Force | Out-Null
}

foreach ($name in $environmentNames) {
    $postmanEnvironment = Join-Path $PSScriptRoot "environments\Env $name.postman_environment.json"
    $reportPath = Join-Path $testsDirectory "postman-Env-$name-$runTimestamp.xml"

    if (-not (Test-Path -LiteralPath $postmanEnvironment -PathType Leaf)) {
        throw "Postman environment not found: $postmanEnvironment"
    }

    Write-Output "Running the local Postman simulation: Env $name"
    & $postmanCommand.Path collection run $collectionPath `
        --environment $postmanEnvironment `
        --reporters cli,junit `
        --reporter-junit-export $reportPath

    if ($LASTEXITCODE -ne 0) {
        throw "The Postman collection failed for Env $name (exit code $LASTEXITCODE)."
    }
}
