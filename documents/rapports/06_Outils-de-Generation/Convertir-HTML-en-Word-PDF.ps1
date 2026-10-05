# ============================================================================
# Organization : Portage Cybertech
# Project      : Mini OAuth 2.0 platform
# Purpose      : Uses Microsoft Word automation to convert the selected bilingual report HTML sources into DOCX and PDF exports, validates generated file presence/size, and replaces companions.
# Author       : Sylvose Allogo <sylvose.allogo@yahoo.com>
# Version      : 1.0.0
# Date         : 2026-10-04
# ============================================================================
$ErrorActionPreference = 'Stop'
$reportsRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$documents = @(
    '02_Architecture\fr\dossier-architecture-portage-cybertech',
    '02_Architecture\fr\guide-modeles-formats-techniques',
    '01_Cadrage\fr\cahier-des-charges-fonctionnel-technique',
    '02_Architecture\fr\architecture-microservices-recommandee',
    '05_Gestion-et-Livrables\fr\choix-technologiques-plateforme',
    '05_Gestion-et-Livrables\fr\referentiel-artefacts-logiciels',
    '02_Architecture\fr\Note-BPEL-Processus-Reference',
    '01_Cadrage\en\functional-and-technical-requirements',
    '02_Architecture\en\oauth2-platform-architecture',
    '02_Architecture\en\recommended-microservices-architecture',
    '02_Architecture\en\bpel-process-reference-note',
    '02_Architecture\fr\description-processus-bpmn-oauth2',
    '02_Architecture\en\oauth2-bpmn-process-description',
    '02_Architecture\en\technical-models-and-formats-guide',
    '04_Tests-et-Validation\en\unit-test-results',
    '04_Tests-et-Validation\en\integration-test-results',
    '04_Tests-et-Validation\fr\rapport-tests-unitaires',
    '04_Tests-et-Validation\fr\rapport-tests-integration',
    '04_Tests-et-Validation\fr\Rapport-des-tests-Endpoint-Postman-Env-Dev',
    '04_Tests-et-Validation\en\Postman-Endpoint-Test-Report-Dev-Environment',
    '04_Tests-et-Validation\fr\Historique-des-demandes-et-reponses-Mini-plateforme-OAuth2',
    '04_Tests-et-Validation\en\Project-Requirements-and-Decisions-OAuth2-Platform',
    '05_Gestion-et-Livrables\en\technology-selection-for-oauth2-platform',
    '05_Gestion-et-Livrables\en\software-artifact-register'
)
$word = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    foreach ($relativeBase in $documents) {
        $basePath = Join-Path $reportsRoot $relativeBase
        $htmlPath = "$basePath.html"
        if (-not (Test-Path -LiteralPath $htmlPath -PathType Leaf)) {
            throw "Required HTML source is missing: $htmlPath"
        }
        $document = $word.Documents.Open($htmlPath, $false, $true)
        try {
            $temporaryBase = "$basePath.generated"
            $document.SaveAs2("$temporaryBase.docx", 16)
            $document.ExportAsFixedFormat("$temporaryBase.pdf", 17)
        } finally {
            $document.Close(0)
        }
        foreach ($extension in @('.docx', '.pdf')) {
            $temporaryOutput = "$basePath.generated$extension"
            $output = "$basePath$extension"
            if (-not (Test-Path -LiteralPath $temporaryOutput -PathType Leaf) -or
                    (Get-Item -LiteralPath $temporaryOutput).Length -lt 1000) {
                throw "Generated document is missing or unexpectedly small: $temporaryOutput"
            }
            if (Test-Path -LiteralPath $output -PathType Leaf) {
                [System.IO.File]::SetAttributes($output, [System.IO.FileAttributes]::Normal)
            }
            Move-Item -LiteralPath $temporaryOutput -Destination $output -Force
        }
        foreach ($output in @("$basePath.docx", "$basePath.pdf")) {
            if (-not (Test-Path -LiteralPath $output -PathType Leaf) -or
                    (Get-Item -LiteralPath $output).Length -lt 1000) {
                throw "Generated document is missing or unexpectedly small: $output"
            }
        }
        Write-Output "Generated Word and PDF companions for $relativeBase"
    }

} finally {
    if ($word) {
        $word.Quit()
        [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
