# ============================================================================
# Organization : Portage Cybertech
# Project      : Mini OAuth 2.0 platform
# Purpose      : Starts the local Resource Server with the trusted issuer, audience, JWKS endpoint, and configured listening port.
# Author       : Sylvose Allogo <sylvose.allogo@yahoo.com>
# Version      : 1.0.0
# Date         : 2026-10-04
# ============================================================================
$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$applicationJar = Join-Path $repositoryRoot 'resource-server\target\resource-server-1.0.0-exec.jar'
if (-not (Test-Path $applicationJar)) {
    throw "JAR Resource Server introuvable : $applicationJar. Construire le projet avec Maven."
}

$env:SERVER_PORT = '8080'
$env:OAUTH_ISSUER = 'http://localhost:9090'
$env:OAUTH_API_AUDIENCE = 'http://localhost:8080/api'
$env:OAUTH_JWKS_URI = 'http://localhost:9090/oauth2/jwks'

Write-Output "Resource Server Postman localhost : http://localhost:8080"
& java -jar $applicationJar
exit $LASTEXITCODE
