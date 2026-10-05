# ============================================================================
# Organization : Portage Cybertech
# Project      : Mini OAuth 2.0 platform
# Purpose      : Builds the configured French and English detailed-design and BPMN document outputs from project sources using the installed document-generation workflow.
# Author       : Sylvose Allogo <sylvose.allogo@yahoo.com>
# Version      : 1.0.0
# Date         : 2026-10-04
# ============================================================================
param([switch] $Force)

$ErrorActionPreference = 'Stop'
$reportsRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$documents = @(
    '03_Conception\fr\conception-detaillee-mini-plateforme-oauth2',
    '03_Conception\en\detailed-design-mini-oauth2-platform',
    '02_Architecture\fr\description-processus-bpmn-oauth2',
    '02_Architecture\en\oauth2-bpmn-process-description',
    '02_Architecture\en\technical-models-and-formats-guide',
    '01_Cadrage\fr\etude-de-faisabilite-portage-cybertech',
    '01_Cadrage\en\project-scope-and-feasibility',
    '04_Tests-et-Validation\fr\rapport-tests-postman',
    '04_Tests-et-Validation\en\oauth2-contract-test-report',
    '05_Gestion-et-Livrables\fr\catalogue-livrables-ecarts',
    '05_Gestion-et-Livrables\en\deliverable-register-and-decisions'
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
        $outputs = @("$basePath.docx", "$basePath.pdf")
        if (-not $Force -and ($outputs | Where-Object {
                    -not (Test-Path -LiteralPath $_ -PathType Leaf) -or
                    (Get-Item -LiteralPath $_).Length -lt 1000
                }).Count -eq 0) {
            Write-Output "Word and PDF documents already exist for $relativeBase"
            continue
        }

        $document = $word.Documents.Open($htmlPath, $false, $true)
        $temporaryBase = "$basePath.generated"
        try {
            $document.SaveAs2("$temporaryBase.docx", 16)
            $document.ExportAsFixedFormat("$temporaryBase.pdf", 17)
        } finally {
            $document.Close(0)
        }

        foreach ($extension in @('.docx', '.pdf')) {
            $temporaryOutput = "$temporaryBase$extension"
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
        Write-Output "Generated Word and PDF documents for $relativeBase"
    }
} finally {
    if ($word) {
        $word.Quit()
        [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
