# ============================================================================
# Organization : Portage Cybertech
# Project      : Mini OAuth 2.0 platform
# Purpose      : Starts the local Authorization Server with the development OAuth issuer, client configuration, token grant settings, and configured listening port.
# Author       : Sylvose Allogo <sylvose.allogo@yahoo.com>
# Version      : 1.0.0
# Date         : 2026-10-04
# ============================================================================
$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$applicationJar = Join-Path $repositoryRoot 'authorization-server\target\authorization-server-1.0.0-exec.jar'
if (-not (Test-Path $applicationJar)) {
    throw "JAR Authorization Server introuvable : $applicationJar. Construire le projet avec Maven."
}

$env:SERVER_PORT = '9090'
$env:OAUTH_ISSUER = 'http://localhost:9090'
$env:OAUTH_API_AUDIENCE = 'http://localhost:8080/api'
$env:OAUTH_ALLOWED_AUDIENCES = 'http://localhost:8080/api'
$env:OAUTH_CLIENT_ID = 'agent-client'
$env:OAUTH_SUBJECT_GRANT_ENABLED = 'true'

Write-Output "Authorization Server Postman localhost : http://localhost:9090"
Write-Output 'Grant subject activé pour la simulation locale uniquement.'
& java -jar $applicationJar
exit $LASTEXITCODE
