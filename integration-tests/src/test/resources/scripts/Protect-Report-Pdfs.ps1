# ============================================================================
# Organization : Portage Cybertech
# Project      : Mini OAuth 2.0 platform
# Purpose      : Accepts a report-tree root, Java class path, protection utility source, and secure opening password; invokes the Java PDFBox utility, forwards the password through standard input, reports tool output, and propagates non-zero failures.
# Author       : Sylvose Allogo <sylvose.allogo@yahoo.com>
# Version      : 1.0.0
# Date         : 2026-10-04
# ============================================================================
# .SYNOPSIS
# Applies password encryption and restricted permissions to PDF reports.
# .DESCRIPTION
# Validates its inputs, prompts securely when no SecureString is supplied,
# and invokes the Java/PDFBox utility. The password is passed over standard
# input, not added to the Java command line. Failures are propagated.
# .PARAMETER ReportsRoot
# Existing report directory whose PDF descendants are to be processed.
# .PARAMETER JavaClassPath
# Java class path containing PDFBox and required runtime dependencies.
# .PARAMETER JavaSource
# Path to the ProtectReportPdfs Java source file.
# .PARAMETER OpeningPassword
# SecureString user password for opening protected PDFs; if omitted, a
# masked interactive prompt is displayed.
param(
    [Parameter(Mandatory = $true)]
    [string] $ReportsRoot,
    [string] $JavaClassPath = (Join-Path $env:TEMP 'pdfbox-app-3.0.8.jar'),
    [Parameter(Mandatory = $true)]
    [string] $JavaSource,
    [System.Security.SecureString] $OpeningPassword
)

$ErrorActionPreference = 'Stop'
$reportsRoot = [System.IO.Path]::GetFullPath($ReportsRoot)
$javaSource = [System.IO.Path]::GetFullPath($JavaSource)

if ([string]::IsNullOrWhiteSpace($JavaClassPath)) {
    throw 'The Java class path for PDFBox cannot be empty.'
}
if (-not (Test-Path -LiteralPath $javaSource -PathType Leaf)) {
    throw "PDF protection source is missing: $javaSource"
}
if (-not (Test-Path -LiteralPath $reportsRoot -PathType Container)) {
    throw "Reports directory not found: $reportsRoot"
}

if ($null -eq $OpeningPassword) {
    $password = Read-Host -Prompt 'Enter the PDF opening password' -AsSecureString
} else {
    $password = $OpeningPassword
}
$passwordPointer = [IntPtr]::Zero
$process = $null
try {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = 'java'
    $startInfo.Arguments = '--class-path "' + $JavaClassPath + '" "' + $javaSource +
        '" "' + $reportsRoot + '"'
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        throw 'Could not start Java for PDF protection.'
    }
    $process.StandardInput.WriteLine($plainPassword)
    $process.StandardInput.Close()
    $output = $process.StandardOutput.ReadToEnd()
    $errors = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    if ($output) {
        Write-Output $output.TrimEnd()
    }
    if ($process.ExitCode -ne 0) {
        throw "PDF protection failed (exit code $($process.ExitCode)): $errors"
    }
} finally {
    $plainPassword = $null
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    if ($process) {
        $process.Dispose()
    }
    $password.Dispose()
}
