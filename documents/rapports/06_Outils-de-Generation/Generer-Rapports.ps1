# ============================================================================
# Organization : Portage Cybertech
# Project      : Mini OAuth 2.0 platform
# Purpose      : Generates project-management, feasibility, test, and deliverable reports from requirements, project metadata, Postman evidence, and report templates.
# Author       : Sylvose Allogo <sylvose.allogo@yahoo.com>
# Version      : 1.0.0
# Date         : 2026-10-04
# ============================================================================
[CmdletBinding()]
param(
    [string] $ReportsRoot = '',
    [string] $SourceBriefPdf = 'C:\TestPortageCyberTech\documents\Portage Cybertech - Cahier des charges - Défi technique.pdf',
    [string] $RequirementsPdf = ''
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($ReportsRoot)) {
    $generatorPath = if ([string]::IsNullOrWhiteSpace($PSScriptRoot)) {
        $MyInvocation.MyCommand.Path
    } else {
        Join-Path $PSScriptRoot 'Generer-Rapports.ps1'
    }
    $ReportsRoot = Split-Path -Parent (Split-Path -Parent $generatorPath)
}
$ReportsRoot = [System.IO.Path]::GetFullPath($ReportsRoot)
if ([string]::IsNullOrWhiteSpace($RequirementsPdf)) {
    $RequirementsPdf = Join-Path $ReportsRoot '01_Cadrage\fr\cahier-des-charges-fonctionnel-technique.pdf'
}
$evidenceDirectory = Join-Path $ReportsRoot '04_Tests-et-Validation\Postman\Preuves'
$postmanDirectory = Join-Path $ReportsRoot '04_Tests-et-Validation\Postman'
$historyDirectory = Join-Path $ReportsRoot '04_Tests-et-Validation\Rapports-historiques'
$collectionDirectory = Join-Path $postmanDirectory 'Scripts'
$postmanCollection = Join-Path $collectionDirectory 'Portage CyberTech - OAuth2 End-to-End.postman_collection.json'
$workDirectory = Join-Path $ReportsRoot '.work-rapports'
$author = 'Sylvose Allogo'
$email = 'sylvose.allogo@yahoo.com'
$reportDate = Get-Date -Format 'yyyy-MM-dd'
$logoPath = Join-Path $ReportsRoot '06_Outils-de-Generation\Assets\Portage-CyberTech-logo-officiel.svg'

foreach ($requiredPath in @($ReportsRoot, $evidenceDirectory, $postmanDirectory, $collectionDirectory)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required report path does not exist: $requiredPath"
    }
}
if (-not (Test-Path -LiteralPath $SourceBriefPdf -PathType Leaf)) {
    throw "Client brief PDF was not found: $SourceBriefPdf"
}
if (-not (Test-Path -LiteralPath $RequirementsPdf -PathType Leaf)) {
    throw "Functional and technical requirements PDF was not found: $RequirementsPdf"
}

New-Item -ItemType Directory -Path $workDirectory -Force | Out-Null
Add-Type -AssemblyName System.Drawing

function Add-Paragraph {
    param(
        [object] $Document,
        [string] $Text,
        [string] $Style = 'Normal',
        [switch] $Centered
    )
    $selection = $script:word.Selection
    $styleId = switch ($Style) {
        'Normal' { -1 }
        'Title' { -63 }
        'Subtitle' { -75 }
        'Heading 1' { -2 }
        'Heading 2' { -3 }
        'Caption' { -35 }
        default { -1 }
    }
    $selection.Style = $Document.Styles.Item($styleId)
    if ($Centered) {
        $selection.ParagraphFormat.Alignment = 1
    } else {
        $selection.ParagraphFormat.Alignment = 0
    }
    $selection.ParagraphFormat.SpaceAfter = 6
    $selection.TypeText($Text)
    $selection.TypeParagraph()
}

function Add-PageBreak {
    $script:word.Selection.InsertBreak(7)
}

function Add-Table {
    param(
        [object] $Document,
        [string[]] $Headers,
        [object[]] $Rows
    )
    $rowCount = $Rows.Count + 1
    $columnCount = $Headers.Count
    $table = $Document.Tables.Add($script:word.Selection.Range, $rowCount, $columnCount)
    $table.Borders.Enable = 1
    $table.Range.Font.Name = 'Aptos'
    $table.Range.Font.Size = 8.5
    $table.Range.ParagraphFormat.SpaceAfter = 2
    $table.Rows.Item(1).Range.Bold = $true
    $table.Rows.Item(1).Range.Shading.BackgroundPatternColor = 15124006
    $table.Rows.Item(1).HeadingFormat = $true
    for ($column = 0; $column -lt $columnCount; $column++) {
        $table.Cell(1, $column + 1).Range.Text = $Headers[$column]
    }
    for ($row = 0; $row -lt $Rows.Count; $row++) {
        for ($column = 0; $column -lt $columnCount; $column++) {
            $value = [string]$Rows[$row][$column]
            $table.Cell($row + 2, $column + 1).Range.Text = $value
            $table.Cell($row + 2, $column + 1).VerticalAlignment = 1
        }
    }
    $script:word.Selection.SetRange($table.Range.End, $table.Range.End)
    $script:word.Selection.TypeParagraph()
}

function Add-ReportCover {
    param([object] $Document, [string] $Title, [string] $Subtitle)
    if (Test-Path -LiteralPath $script:logoPath -PathType Leaf) {
        $logo = $script:word.Selection.InlineShapes.AddPicture($script:logoPath, $false, $true)
        $logo.LockAspectRatio = -1
        $logo.Width = 131
        $script:word.Selection.TypeParagraph()
    }
    Add-Paragraph $Document 'PORTAGE CYBERTECH' 'Subtitle' -Centered
    Add-Paragraph $Document $Title 'Title' -Centered
    Add-Paragraph $Document $Subtitle 'Subtitle' -Centered
    Add-Paragraph $Document ''
    Add-Table $Document @('Métadonnée', 'Valeur') @(
        ,@('Organisation', 'Portage CyberTech'),
        ,@('Projet', 'Mini-plateforme OAuth 2.0 — Authorization and Resource Server Contract Tests'),
        ,@('Version du projet', '1.0.0'),
        ,@('Auteur', "$script:author <$script:email>"),
        ,@('Version du rapport', '1.0'),
        ,@('Date de production', $script:reportDate),
        ,@('Statut des preuves', 'Captures et résultats dʼexécution historiques; non assimilés à un nouveau test')
    )
    Add-Paragraph $Document 'Document de livraison — diffusion contrôlée si des preuves brutes sont jointes.' 'Subtitle'
    Add-PageBreak
}

function New-ReportDocument {
    param([string] $Title)
    $document = $script:word.Documents.Add()
    $document.PageSetup.TopMargin = 54
    $document.PageSetup.BottomMargin = 54
    $document.PageSetup.LeftMargin = 60
    $document.PageSetup.RightMargin = 60
    $document.Styles.Item(-1).Font.Name = 'Aptos'
    $document.Styles.Item(-1).Font.Size = 10
    $document.Styles.Item(-1).ParagraphFormat.SpaceAfter = 6
    $document.Styles.Item(-2).Font.Color = 8210719
    $document.Styles.Item(-2).Font.Name = 'Aptos Display'
    $document.Styles.Item(-2).Font.Size = 17
    $document.Styles.Item(-3).Font.Color = 10498160
    $document.Styles.Item(-3).Font.Name = 'Aptos Display'
    $document.Styles.Item(-3).Font.Size = 13
    $footer = $document.Sections.Item(1).Footers.Item(1).Range
    $footer.Text = "Portage CyberTech  |  $script:author  |  $script:reportDate  |  Page "
    $footer.Fields.Add($footer, 33) | Out-Null
    $footer.ParagraphFormat.Alignment = 1
    return $document
}

function Save-ReportDocument {
    param([object] $Document, [string] $Path)
    $temporaryPath = Join-Path (Split-Path -Parent $Path) `
        ([System.IO.Path]::GetFileNameWithoutExtension($Path) + '.generated.docx')
    $temporaryPdfPath = [System.IO.Path]::ChangeExtension($temporaryPath, '.pdf')
    $pdfPath = [System.IO.Path]::ChangeExtension($Path, '.pdf')
    $Document.SaveAs2($temporaryPath, 16)
    $Document.ExportAsFixedFormat($temporaryPdfPath, 17)
    $Document.Close(0)
    foreach ($pair in @(
            @{ Temporary = $temporaryPath; Output = $Path },
            @{ Temporary = $temporaryPdfPath; Output = $pdfPath }
        )) {
        if (-not (Test-Path -LiteralPath $pair.Temporary -PathType Leaf) -or
                (Get-Item -LiteralPath $pair.Temporary).Length -lt 1000) {
            throw "Generated report is missing or unexpectedly small: $($pair.Temporary)"
        }
        if (Test-Path -LiteralPath $pair.Output -PathType Leaf) {
            [System.IO.File]::SetAttributes($pair.Output, [System.IO.FileAttributes]::Normal)
        }
        Move-Item -LiteralPath $pair.Temporary -Destination $pair.Output -Force
    }
    Write-Output "Generated $Path"
    Write-Output "Generated $pdfPath"
}

function Add-SanitizedEvidence {
    param([string] $SourcePath, [string] $Label)
    $destination = Join-Path $script:workDirectory ([guid]::NewGuid().ToString('N') + '.png')
    $image = [System.Drawing.Bitmap]::new($SourcePath)
    $graphics = [System.Drawing.Graphics]::FromImage($image)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 36, 44, 58))
    $font = [System.Drawing.Font]::new('Arial', [single]12, [System.Drawing.FontStyle]::Bold)
    $textBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
    $redactTop = $false
    $redactBottom = $false
    if ($SourcePath -match '01 - Authorization Server - health|02 - Resource Server - health') {
        $redactTop = $true
    }
    if ($SourcePath -match '04 - Authorization Server - local demo source token|05 - Authorization Server - RFC 8693 token exchange') {
        $redactTop = $true
        $redactBottom = $true
    }
    if ($SourcePath -match '06 - Resource Server|07 - Resource Server|08 - Resource Server|09 - Authorization Server|10 - Authorization Server|Demo des endpoints|Start Micro Service') {
        $redactBottom = $true
    }
    if ($redactTop) {
        $rectangle = [System.Drawing.RectangleF]::new(
            [single]($image.Width * 0.28),
            [single]($image.Height * 0.48),
            [single]($image.Width * 0.72),
            [single]($image.Height * 0.25))
        $graphics.FillRectangle($brush, $rectangle)
        $graphics.DrawString('REPONSE / VALEUR SENSIBLE MASQUEE', $font, $textBrush, $rectangle.X + 12, $rectangle.Y + 12)
    }
    if ($redactBottom) {
        $rectangle = [System.Drawing.RectangleF]::new(
            0,
            [single]($image.Height * 0.75),
            [single]$image.Width,
            [single]($image.Height * 0.25))
        $graphics.FillRectangle($brush, $rectangle)
        $graphics.DrawString('EN-TETES ET JETONS MASQUES', $font, $textBrush, 16, $rectangle.Y + 12)
    }
    $image.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
    $text = "Figure — $Label (copie expurgée; les identifiants, jetons et en-têtes dʼauthentification sont masqués)."
    $graphics.Dispose()
    $brush.Dispose()
    $font.Dispose()
    $textBrush.Dispose()
    $image.Dispose()

    Add-Paragraph $script:currentDocument $text 'Caption'
    $shape = $script:word.Selection.InlineShapes.AddPicture($destination, $false, $true)
    $shape.LockAspectRatio = -1
    if ($shape.Width -gt 490) {
        $shape.Width = 490
    }
    $script:word.Selection.TypeParagraph()
}

function Get-RelativeReportPath {
    param([string] $FullName)
    return $FullName.Substring($script:ReportsRoot.Length).TrimStart('\')
}

$script:word = $null
$script:currentDocument = $null
try {
    $script:word = New-Object -ComObject Word.Application
    $script:word.Visible = $false
    $script:word.DisplayAlerts = 0

    $collection = Get-Content -LiteralPath $postmanCollection -Raw | ConvertFrom-Json
    $historicalRuns = @()
    foreach ($environment in @('Test', 'Int', 'Prod')) {
        $xmlPath = Join-Path $historyDirectory "Postman\postman-Env-$environment.xml"
        if (-not (Test-Path -LiteralPath $xmlPath)) {
            throw "Historical Postman JUnit evidence is missing: $xmlPath"
        }
        [xml]$xml = Get-Content -LiteralPath $xmlPath -Raw
        $root = $xml.testsuites
        $historicalRuns += [pscustomobject]@{
            Environment = $environment
            Tests = [int]$root.tests
            Failures = [int]$root.failures
            Errors = [int]$root.errors
            Timestamp = [string]$root.testsuite[0].timestamp
        }
    }

    $testCases = @(
        [pscustomobject]@{ Id='01'; File='01 - Authorization Server - health.png'; Title='Disponibilité de lʼAuthorization Server'; Method='GET'; Endpoint='/actuator/health'; Expected='200; état UP'; Assertions='Le statut HTTP vaut 200 et le JSON contient status=UP.'; Result='200 OK; capture Postman avec assertion réussie.'; Time='9 ms dans la capture'; Objective='Vérifier le démarrage et la disponibilité de lʼAuthorization Server.' },
        [pscustomobject]@{ Id='02'; File='02 - Resource Server - health.png'; Title='Disponibilité du Resource Server'; Method='GET'; Endpoint='/actuator/health'; Expected='200; état UP'; Assertions='Le statut HTTP vaut 200 et le JSON contient status=UP.'; Result='200 OK dans la preuve visuelle.'; Time='Non relevé dans le compte rendu'; Objective='Vérifier séparément la disponibilité du service de ressources.' },
        [pscustomobject]@{ Id='03'; File='03 - Authorization Server - JWKS public.png'; Title='Publication du JWKS public'; Method='GET'; Endpoint='/oauth2/jwks'; Expected='200; JWK RSA utilisable'; Assertions='Au moins une clé RSA comporte kid, n et e; les membres privés d, p, q, dp, dq, qi et oth sont absents.'; Result='200 OK; réponse JWKS visible et assertion 1/1 réussie.'; Time='Non relevé dans le compte rendu'; Objective='Contrôler le contrat de découverte de la clé publique sans divulgation du matériel privé.' },
        [pscustomobject]@{ Id='04'; File='04 - Authorization Server - local demo source token (localhost simulation only).png'; Title='Émission du jeton source de démonstration'; Method='POST'; Endpoint='/oauth2/token'; Expected='200; Bearer JWT, durée 300 s'; Assertions='Le grant local émet un access_token; token_type vaut Bearer et le scope demandé est présent.'; Result='200 OK; expires_in=300 visible; jeton masqué dans le rapport.'; Time='143 ms dans la capture'; Objective='Préparer le subject_token nécessaire à lʼessai dʼéchange. Ce grant local ne vérifie pas lʼidentité du sujet.' },
        [pscustomobject]@{ Id='05'; File='05 - Authorization Server - RFC 8693 token exchange.png'; Title='Échange de jeton RFC 8693'; Method='POST'; Endpoint='/oauth2/token'; Expected='200; access token de type Bearer'; Assertions='La requête comprend subject_token, types de jeton, audience et scope; la réponse contient un JWT compact en trois segments.'; Result='200 OK; assertion de lʼéchange réussie; corps contenant le JWT expurgé.'; Time='84 ms dans la capture'; Objective='Vérifier le contrat dʼéchange et récupérer le jeton destiné à lʼAPI.' },
        [pscustomobject]@{ Id='06'; File='06 - Resource Server - protected hello with OAuth2 Bearer.png'; Title='Accès authentifié à HelloWorld'; Method='GET'; Endpoint='/api/hello'; Expected='200; message de salutation'; Assertions='Le jeton échangé donne accès à la ressource et le sujet apparaît dans message.'; Result='200 OK; “Hello World postman-local-smoke” dans la capture; jeton masqué.'; Time='107 ms dans la capture'; Objective='Confirmer lʼinteropérabilité du JWT émis avec la politique du Resource Server.' },
        [pscustomobject]@{ Id='07'; File='07 - Resource Server - rejects missing'; Title='Refus dʼun jeton absent'; Method='GET'; Endpoint='/api/hello'; Expected='401 Unauthorized'; Assertions='Une requête sans Authorization Bearer est refusée.'; Result='401 attendu dans la collection et résultat 1/1 visible.'; Time='Non relevé dans le compte rendu'; Objective='Vérifier le refus par défaut dʼune requête anonyme.' },
        [pscustomobject]@{ Id='08'; File='08 - Resource Server - rejects modified JWT signature.png'; Title='Refus dʼune signature JWT modifiée'; Method='GET'; Endpoint='/api/hello'; Expected='401 Unauthorized'; Assertions='La collection modifie un caractère de la signature et sʼassure que le jeton altéré est différent du jeton original.'; Result='401 Unauthorized dans la capture; jeton et en-têtes masqués.'; Time='5 ms dans la capture'; Objective='Contrôler la validation cryptographique de la signature du JWT.' },
        [pscustomobject]@{ Id='09'; File='09 - Authorization Server - rejects invalid client.png'; Title='Rejet des identifiants du client OAuth'; Method='POST'; Endpoint='/oauth2/token'; Expected='401; error=invalid_client'; Assertions='Un secret volontairement incorrect ne permet pas lʼauthentification du client.'; Result='401 Unauthorized; invalid_client visible; en-tête HTTP Basic masqué.'; Time='76 ms dans la capture'; Objective='Vérifier lʼauthentification du client confidentiel avant traitement du grant.' },
        [pscustomobject]@{ Id='10'; File='10 - Authorization Server - rejects invalid RFC 8693 source token.png'; Title='Rejet du jeton source invalide'; Method='POST'; Endpoint='/oauth2/token'; Expected='400; error=invalid_grant'; Assertions='Le jeton source malformé nʼest pas accepté par le grant RFC 8693.'; Result='400 Bad Request; invalid_grant visible; en-tête HTTP Basic masqué.'; Time='73 ms dans la capture'; Objective='Vérifier le rejet dʼun jeton source qui ne peut pas être validé.' }
    )

    $script:currentDocument = New-ReportDocument 'Rapport des tests dʼendpoints avec Postman'
    Add-ReportCover $script:currentDocument 'Rapport de tests des endpoints avec Postman' 'Authorization Server et Resource Server — preuves, assertions et limites'
    Add-Paragraph $script:currentDocument '1. Synthèse exécutive' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Le dossier de preuves décrit un parcours local en dix requêtes : disponibilité des deux services, publication JWKS, émission dʼun jeton de démonstration, échange RFC 8693, accès protégé, puis quatre contrôles négatifs. Les dix captures montrent les assertions Postman associées; trois rapports JUnit stockés enregistrent chacun 10 tests, 0 échec et 0 erreur.'
    Add-Paragraph $script:currentDocument 'Les rapports JUnit sont datés du 3 octobre 2026 à 05:08 UTC. Ils constituent une preuve historique du run enregistré, pas un résultat produit à la date de génération de ce document. Les profils Dev, Int et Prod pointent tous vers localhost; leurs noms ne démontrent pas lʼexistence dʼenvironnements distants.'
    Add-Paragraph $script:currentDocument '2. Objet, périmètre et méthode' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Objectif : vérifier les contrats HTTP visibles côté client pour lʼémission et lʼéchange de jetons OAuth 2.0, la validation du JWT par le Resource Server, lʼexposition limitée du JWKS et le comportement de refus. Le périmètre est celui de la collection « Portage CyberTech - OAuth2 End-to-End » (Postman Collection v2.1.0), de ses scripts de test, des images du dossier Resultats et des fichiers JUnit XML archivés.'
    Add-Paragraph $script:currentDocument 'Méthode de rapprochement : les scénarios et assertions sont lus depuis le JSON de la collection; les statuts et durées sont relevés des captures lorsquʼils sont lisibles; les nombres et horodatages sont contrôlés dans les XML JUnit. Les valeurs dʼaccès (JWT, HTTP Basic) sont masquées dans les copies intégrées au rapport. Les captures originales sont conservées à accès contrôlé.'
    Add-Paragraph $script:currentDocument 'Outils et versions observés' 'Heading 2'
    Add-Table $script:currentDocument @('Outil / élément', 'Version ou configuration', 'Source / précision') @(
        ,@('Postman Collection', 'Schéma v2.1.0', 'Champ info.schema du JSON livré'),
        ,@('Postman Runtime', '7.34.0', 'User-Agent lisible dans les captures; version de lʼapplication Desktop non relevée'),
        ,@('Postman CLI', 'Version non enregistrée', 'Runner PowerShell présent; aucun nouveau lancement CLI attesté par ce rapport'),
        ,@('Authorization Server', 'Spring Boot 3.3.4; port local 9090', 'POM et profils de simulation'),
        ,@('Resource Server', 'Spring Boot 3.3.4; port local 8080', 'POM et profils de simulation'),
        ,@('Java / Maven', 'Java 23 déclaré; Maven 3.10.0 disponible sur le poste de génération', 'POM racine et environnement local; la version de lʼexécution historique nʼest pas incluse dans les XML'),
        ,@('Transport de la simulation', 'HTTP localhost; HTTP Basic côté token endpoint; Bearer côté API', 'Environnements Postman et captures')
    )
    Add-Paragraph $script:currentDocument '3. Matrice de résultats Postman' 'Heading 1'
    $summaryRows = foreach ($test in $testCases) {
        ,@($test.Id, $test.Method, $test.Endpoint, $test.Expected, $test.Result, $test.Time)
    }
    Add-Table $script:currentDocument @('ID', 'Méthode', 'Endpoint', 'Résultat attendu', 'Résultat observé dans la preuve', 'Durée capturée') @($summaryRows)
    Add-Paragraph $script:currentDocument '4. Détail des scénarios et preuves' 'Heading 1'
    foreach ($test in $testCases) {
        Add-PageBreak
        Add-Paragraph $script:currentDocument "$($test.Id) — $($test.Title)" 'Heading 2'
        Add-Paragraph $script:currentDocument "But : $($test.Objective)"
        Add-Table $script:currentDocument @('Rubrique', 'Détail') @(
            ,@('Requête', "$($test.Method) $($test.Endpoint)"),
            ,@('Résultat attendu', $test.Expected),
            ,@('Assertions de la collection', $test.Assertions),
            ,@('Résultat documenté', $test.Result),
            ,@('Temps de réponse', $test.Time),
            ,@('Environnement', 'Simulation locale : Authorization Server localhost:9090; Resource Server localhost:8080')
        )
        $imagePath = if ($test.Id -eq '07') {
            Get-ChildItem -LiteralPath $evidenceDirectory -File | Where-Object { $_.Name.StartsWith($test.File) } | Select-Object -First 1 -ExpandProperty FullName
        } else {
            Join-Path $evidenceDirectory $test.File
        }
        if (-not $imagePath -or -not (Test-Path -LiteralPath $imagePath -PathType Leaf)) {
            throw "Evidence image is missing for scenario $($test.Id): $($test.File)"
        }
        Add-SanitizedEvidence $imagePath "$($test.Id) — $($test.Title)"
    }
    Add-PageBreak
    Add-Paragraph $script:currentDocument 'Annexe A — Démarrage et état des services' 'Heading 1'
    $supplementaryImages = @(
        [pscustomobject]@{ File='Demo des endpoints dans Postman.png'; Title='Vue dʼensemble de la collection Postman' },
        [pscustomobject]@{ File='Start Micro Service Authorization Server.png'; Title='Lancement local de lʼAuthorization Server' },
        [pscustomobject]@{ File='Start Micro Service Resource Server.png'; Title='Lancement local du Resource Server' },
        [pscustomobject]@{ File='Test Health Spring Boot Authorization Server + Resource Server.png'; Title='Vérification combinée de santé Spring Boot' },
        [pscustomobject]@{ File='Test Health Spring Boot Authorization Server.png'; Title='Vérification de santé — Authorization Server' },
        [pscustomobject]@{ File='Test Health Spring Boot Resource Server.png'; Title='Vérification de santé — Resource Server' }
    )
    foreach ($evidence in $supplementaryImages) {
        Add-PageBreak
        Add-Paragraph $script:currentDocument $evidence.Title 'Heading 2'
        $imagePath = Join-Path $evidenceDirectory $evidence.File
        if (-not (Test-Path -LiteralPath $imagePath -PathType Leaf)) {
            throw "Supplementary evidence image is missing: $imagePath"
        }
        Add-SanitizedEvidence $imagePath $evidence.Title
    }
    Add-PageBreak
    Add-Paragraph $script:currentDocument '5. Rapports JUnit conservés' 'Heading 1'
    $historicalRows = foreach ($run in $historicalRuns) {
        ,@("Env $($run.Environment)", "$($run.Tests)", "$($run.Failures)", "$($run.Errors)", $run.Timestamp)
    }
    Add-Table $script:currentDocument @('Profil enregistré', 'Tests', 'Échecs', 'Erreurs', 'Horodatage UTC du premier scénario') @($historicalRows)
    Add-Paragraph $script:currentDocument 'Chaque XML contient dix cas (un par requête), sans échec ni erreur. Les trois horodatages diffèrent de quelques secondes et sont des résultats archivés; la ressemblance des profils ne constitue pas une validation dʼenvironnements distincts.'
    Add-Paragraph $script:currentDocument '6. Couverture, limites et suites de test' 'Heading 1'
    Add-Table $script:currentDocument @('Couvert par cette collection', 'Hors de cette collection / à compléter') @(
        ,@('Health 200; JWKS RSA public sans paramètres privés; émission locale et échange RFC 8693; accès valide à /api/hello; jeton absent; signature altérée; mauvais secret client; jeton source malformé.', 'Le besoin distinct dʼaccepter côté Resource Server un JWT signé avec une nouvelle clé nʼest pas démontré par ces dix requêtes.'),
        ,@('Réponses négatives observées : 401 pour le Bearer absent/altéré et le client invalide; 400 invalid_grant pour le jeton source invalide.', 'Ajouter un test dʼintégration RS avec seconde clé de confiance/kid, ainsi que le cas HTTP 403 dʼun JWT valide dépourvu de api.read.'),
        ,@('Vérifications de contrat dans la collection et preuve visuelle de 1/1 assertion par requête.', 'Les captures nʼétablissent ni une durée de charge, ni une disponibilité continue, ni le comportement après expiration/revocation, ni la sécurisation TLS en environnement distant.')
    )
    Add-Paragraph $script:currentDocument '7. Sécurité et traitement des preuves' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Le grant subject local signe un subject choisi par lʼappelant et ne réalise pas dʼauthentification utilisateur. Les captures brutes peuvent afficher des jetons ou des valeurs HTTP Basic encodées : ne pas les envoyer hors de lʼespace autorisé; invalider les jetons de démonstration si leurs signatures restent acceptables. Les images insérées ici sont des copies expurgées. Les profils Env Int et Env Prod sont des simulations localhost, pas des environnements de production.'
    Add-Paragraph $script:currentDocument '8. Références' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Collection : 04_Tests-et-Validation\Postman\Scripts\Portage CyberTech - OAuth2 End-to-End.postman_collection.json. Profils : Scripts\environments\. Exécuteur et démarrage local : Scripts\*.ps1. Preuves : Postman\Preuves\. Résultats historiques : Rapports-historiques\Postman\postman-Env-*.xml.'
    $testReport = Join-Path $ReportsRoot '04_Tests-et-Validation\fr\rapport-tests-postman.docx'
    Save-ReportDocument $script:currentDocument $testReport
    $script:currentDocument = $null

    $script:currentDocument = New-ReportDocument 'Étude de faisabilité — Mini-plateforme OAuth 2.0'
    Add-ReportCover $script:currentDocument 'Étude de faisabilité' 'Défi technique Java / Spring Boot — OAuth 2.0 et validation de JWT'
    Add-Paragraph $script:currentDocument '1. Résumé de décision' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Avis : projet techniquement faisable et cœur de démonstration déjà réalisé, sous réserve dʼune exigence dʼacceptation encore ouverte. Le besoin prévoit explicitement que le Resource Server vérifie aussi un jeton signé avec une nouvelle clé. Les livrables examinés identifient que le test de clé supplémentaire concerne le décodeur de jeton source de lʼAuthorization Server et ne prouve pas lʼacceptation de cette clé par le Resource Server. Lʼacceptation complète doit donc rester conditionnelle à une implémentation et à un test de contrat dédiés, ou à une décision formelle de changement de portée.'
    Add-Paragraph $script:currentDocument 'Conclusion décisionnelle : poursuivre en corrigeant et démontrant ce point; ne pas déclarer cette exigence conforme sur la seule base de la suite Postman actuelle.'
    Add-Paragraph $script:currentDocument '2. Sources et démarche' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Cette étude sʼappuie sur le « Portage Cybertech - Cahier des charges - Défi technique.pdf » (2 pages), le « Cahier des charges fonctionnel et technique » (version 1.0, dérivé du PDF), le code et les livrables dʼarchitecture décrits dans le dépôt, la collection Postman et ses résultats historiques. Le document source prévaut pour déterminer lʼobligation; les choix additionnels du code ou les compléments de vérification sont distingués du besoin initial.'
    Add-Table $script:currentDocument @('Dimension', 'Évaluation', 'Motif') @(
        ,@('Faisabilité technique', 'Favorable avec condition', 'Java/Spring Boot, deux services REST et JWT signé correspondent aux compétences et technologies demandées. Le gap de nouvelle clé RS est circonscrit et vérifiable.'),
        ,@('Faisabilité fonctionnelle', 'Favorable pour le cœur; acceptation partielle tant que REQ-04 est ouverte', 'Lʼémission, la ressource protégée et le parcours Postman existent; la couverture de la nouvelle clé côté RS reste à établir.'),
        ,@('Faisabilité opérationnelle', 'Favorable pour une démonstration locale', 'Scripts, profils localhost, conteneurs et configurations de pipeline documentés; aucune infrastructure distante ou production ne fait partie des preuves.'),
        ,@('Faisabilité économique', 'Non chiffrable avec les données disponibles', 'Aucun taux journalier, budget, coût dʼhébergement, SLA ou cible dʼexploitation nʼest fourni. Un effort indicatif est proposé sans le convertir en montant.'),
        ,@('Adéquation au défi', 'Favorable si les limites sont expliquées', 'La portée convient à un exercice évaluant OAuth/JWT, Spring Security, testabilité, architecture et explication des choix; les limites du grant local doivent être dites.')
    )
    Add-Paragraph $script:currentDocument '3. Besoin du client et critères structurants' 'Heading 1'
    Add-Table $script:currentDocument @('Besoin exprimé dans le PDF', 'Critère de succès interprété') @(
        ,@('Deux API REST, une Authorization Server et une Resource Server.', 'Deux applications Java/Spring Boot distinctes et exécutables; contrats et ports de démonstration reproductibles.'),
        ,@('LʼAuthorization Server émet un JWT signé via une requête POST comportant le sujet.', 'Émission vérifiable cryptographiquement et réponse exploitable par le client; ne pas présenter un subject de démonstration comme identité authentifiée.'),
        ,@('La Resource Server expose une ressource HelloWorld sécurisée et vérifie les jetons.', 'Un JWT valable donne une réponse positive; un jeton absent, altéré, expiré ou rejeté donne un refus contrôlé.'),
        ,@('La Resource Server accepte un jeton signé avec une nouvelle clé pour simuler une rotation, sans exiger la rotation complète côté Authorization Server.', 'Un test de contrat côté Resource Server avec une seconde clé/kid doit passer; ce critère est actuellement identifié comme un gap.'),
        ,@('Démonstration de bout en bout par Postman ou outil équivalent; Java/Spring Boot.', 'Collection importable, procédure de démarrage, assertions, preuves horodatées, version des outils et limites documentées.')
    )
    Add-Paragraph $script:currentDocument 'Le PDF nʼimpose pas RFC 8693, la portée api.read, une base de données, une identité externe, un fournisseur de clés, des ports précis, la production ou un moteur BPEL. Ces éléments sont des choix ou livrables complémentaires; ils ne remplacent pas le critère explicite de nouvelle clé vérifiée par le Resource Server.'
    Add-Paragraph $script:currentDocument '4. Faisabilité de la solution et écarts constatés' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Le découpage en deux applications Spring Boot séparées est adapté aux frontières demandées. Le flux JWT permet au Resource Server de vérifier la signature et les claims sans appeler lʼAuthorization Server à chaque requête; le JWKS public fournit la clé de vérification. Le parcours couvre également un échange RFC 8693 additionnel, ce qui est cohérent avec la démonstration mais élargit le périmètre.'
    Add-Table $script:currentDocument @('Élément', 'État des preuves', 'Décision') @(
        ,@('Émission et échange local', 'Collection Postman, tests de contrat et documentation présents.', 'Acceptable pour la simulation; le grant subject local nʼauthentifie pas le sujet.'),
        ,@('HelloWorld protégé', 'Réponse positive avec Bearer et refus de jeton absent/signature altérée.', 'Réalisé pour le chemin principal.'),
        ,@('Nouvelle clé côté Resource Server', 'Pas de preuve de bout en bout dans la collection; le dossier dʼexigences marque lʼécart explicitement.', 'Non accepté avant test RS dédié ou décision de changement de portée signée.'),
        ,@('Gestion de clés et dʼidentifiants en production', 'Clé et registre client dʼexemple en mémoire; secret de démonstration connu.', 'Hors périmètre de lʼexercice; bloquant avant usage partagé/production.'),
        ,@('Documentation et modèles', 'Cahier des charges, architecture, BPMN, DMN, références BPEL/WSDL, diagrammes et rapports présents.', 'Utiles pour lʼexplication; BPEL/DMN sont des artefacts de référence, pas des moteurs exécutés.')
    )
    Add-Paragraph $script:currentDocument '5. Options de décision sur le point ouvert' 'Heading 1'
    Add-Table $script:currentDocument @('Option', 'Avantage', 'Limite / décision requise') @(
        ,@('A — Implémenter une seconde clé de confiance au Resource Server et un test de contrat (recommandée)', 'Répond directement à lʼénoncé; fournit une preuve indépendante côté consommateur.', 'Confirmer format de configuration, sélection kid, source de confiance, rafraîchissement et critères de refus. Ne pas importer une clé arbitraire.'),
        ,@('B — Demander un amendement formel de lʼexigence', 'Évite une implémentation supplémentaire si lʼévaluateur retire le critère.', 'Jusquʼà acceptation écrite, le besoin PDF reste la référence et le critère est ouvert.'),
        ,@('C — Marquer lʼexistant conforme sur la base dʼAdditionalSigningKeyTest', 'Aucun coût de réalisation immédiat.', 'Non recommandé : le test vérifie le décodeur de jeton source de lʼAuthorization Server, pas le Resource Server.')
    )
    Add-Paragraph $script:currentDocument '6. Risques majeurs et mesures' 'Heading 1'
    $riskPath = Join-Path $ReportsRoot '05_Gestion-et-Livrables\Registre-des-risques.csv'
    $riskRows = Import-Csv -LiteralPath $riskPath -Delimiter ';' | ForEach-Object { ,@($_.ID, $_.Niveau, $_.Risque, $_.PSObject.Properties['Mesure ou decision'].Value, $_.Etat) }
    Add-Table $script:currentDocument @('ID', 'Niveau', 'Risque', 'Mesure principale', 'État') @($riskRows)
    Add-Paragraph $script:currentDocument '7. Plan indicatif de réalisation' 'Heading 1'
    Add-Table $script:currentDocument @('Lot', 'Travaux', 'Effort indicatif', 'Critère de sortie') @(
        ,@('1. Clarifier la confiance de clé', 'Décider clé de configuration ou JWKS additionnel, rotation/kid, environnement et preuve attendue.', '0,5–1 jour', 'Décision dʼarchitecture et critères dʼacceptation approuvés.'),
        ,@('2. Implémenter le chemin RS à nouvelle clé', 'Ajouter la configuration minimale de confiance, valider signature/issuer/audience/exp/scope comme défini et traiter les cas négatifs.', '1–3 jours', 'Une nouvelle clé valide est acceptée; mauvaise/non fiable est rejetée.'),
        ,@('3. Test de contrat et non-régression', 'Exercer HTTP réel avec seconde clé; conserver les scénarios standard et lʼinteropérabilité JWKS.', '1–2 jours', 'Suite ciblée et complète réussie; rapport daté et reproductible.'),
        ,@('4. Mise à jour documentation/remise', 'Actualiser traçabilité, OpenAPI/configuration/README et rapport de test; faire relire les limites.', '0,5–1 jour', 'REQ-04 marquée conforme uniquement avec preuves jointes.')
    )
    Add-Paragraph $script:currentDocument 'Ces durées sont des ordres de grandeur pour une personne familière de Spring Security, hors approvisionnement dʼinfrastructure, approbations, intégration à un fournisseur externe et durcissement de production. Le délai calendaire doit être recalé après décision de confiance et disponibilité des environnements.'
    Add-Paragraph $script:currentDocument '8. Conclusion et conditions de passage' 'Heading 1'
    Add-Paragraph $script:currentDocument 'La mini-plateforme est réalisable avec la pile existante et plusieurs livrables attestent le parcours principal. Lʼétude est favorable sous condition; le projet nʼest pas intégralement conforme à lʼénoncé tant que REQ-04 nʼa pas été démontrée sur le Resource Server. Avant acceptation, obtenir la décision sur la clé, produire un test de contrat positif et négatif avec la nouvelle clé, rerun la suite, joindre un rapport horodaté et faire valider le résultat par le commanditaire. Une mise en production exigerait en plus un chantier distinct de secrets, clés persistantes, TLS, durcissement, supervision et exploitation.'
    Add-Paragraph $script:currentDocument '9. Références' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Source client : C:\TestPortageCyberTech\documents\Portage Cybertech - Cahier des charges - Défi technique.pdf. Spécification détaillée : 01_Cadrage\fr\cahier-des-charges-fonctionnel-technique.pdf et Word. Architecture constatée : 02_Architecture\fr\dossier-architecture-portage-cybertech.pdf. Traçabilité : 05_Gestion-et-Livrables\Matrice-de-tracabilite-exigences.csv.'
    $feasibilityReport = Join-Path $ReportsRoot '01_Cadrage\fr\etude-de-faisabilite-portage-cybertech.docx'
    Save-ReportDocument $script:currentDocument $feasibilityReport
    $script:currentDocument = $null

    $script:currentDocument = New-ReportDocument 'Catalogue des livrables — Portage CyberTech'
    Add-ReportCover $script:currentDocument 'Catalogue des livrables et rapport dʼorganisation' 'Inventaire des artefacts, classement cible, réserves et compléments de génie logiciel'
    Add-Paragraph $script:currentDocument '1. Synthèse et organisation' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Le dossier rapports est classé par cycle de livraison : cadrage, architecture, conception, vérification, gestion des livrables et outils de génération. Lʼinventaire détaillé qui suit est établi à partir des fichiers effectivement présents au moment de la génération; il nʼaffirme pas quʼun artefact natif est exécutable ou quʼun résultat historique reste reproductible.'
    Add-Table $script:currentDocument @('Ordre / dossier', 'Rôle', 'Artefacts principaux') @(
        ,@('01_Cadrage\fr et en', 'Besoin, faisabilité, périmètre et décision', 'Cahier des charges et étude de faisabilité français; synthèse anglaise; exports de lecture.'),
        ,@('02_Architecture\fr et en', 'Vues et architecture de solution', 'Dossier et recommandations français; processus BPMN et guide des formats bilingues; modèles natifs conservés.'),
        ,@('03_Conception\fr et en', 'Conception détaillée', 'Dossiers HTML/Word/PDF en français et anglais; sources PlantUML conservées.'),
        ,@('04_Tests-et-Validation\fr et en', 'Vérification et preuves', 'Rapports Postman bilingues; collection, profils, scripts, captures et résultats historiques.'),
        ,@('05_Gestion-et-Livrables\fr et en', 'Traçabilité, risques et remise', 'Rapports français, registre anglais, matrices exigences-tests et risques au format CSV.'),
        ,@('06_Outils-de-Generation\fr et en', 'Reproductibilité des documents', 'Scripts de génération et index de langue; logo officiel conservé comme ressource.')
    )
    Add-Paragraph $script:currentDocument '2. Inventaire exhaustif des fichiers du dossier rapports' 'Heading 1'
    $allFiles = @(Get-ChildItem -LiteralPath $ReportsRoot -Recurse -File |
        Where-Object { $_.FullName -notmatch '[\\/]\.work-(rapports|verify)[\\/]' -and -not $_.Name.StartsWith('~$') } |
        Sort-Object FullName)
    $inventoryRows = foreach ($file in $allFiles) {
        $relativePath = Get-RelativeReportPath $file.FullName
        $extension = if ([string]::IsNullOrWhiteSpace($file.Extension)) { 'sans extension' } else { $file.Extension.TrimStart('.').ToUpperInvariant() }
        $area = ($relativePath -split '\\')[0]
        $role = switch -Regex ($relativePath) {
            '^01_Cadrage\\fr\\cahier-des-charges-' { 'Cahier des charges fonctionnel et technique en français'; break }
            '^01_Cadrage\\fr\\etude-de-faisabilite-' { 'Étude de faisabilité française et décision conditionnelle'; break }
            '^01_Cadrage\\en\\' { 'Project scope and feasibility report in English'; break }
            '^02_Architecture\\fr\\|^02_Architecture\\en\\' { 'Human-readable architecture and process report'; break }
            '^02_Architecture\\.*\\Scripts\\' { 'Script source de génération architecture/PowerDesigner'; break }
            '^02_Architecture\\.*\\Tests\\' { 'Matrice de décision/test de conception'; break }
            '^02_Architecture\\.*\\diagrams\\|^02_Architecture\\.*\\powerdesigner\\.*\\(Diagrammes|Exports-natifs)\\' { 'Diagramme ou export visuel de lʼarchitecture'; break }
            '^02_Architecture\\.*\\powerdesigner\\.*\\\.(bpm|oom|eam)$|^02_Architecture\\.*\\(bpmn|uml|eam)\\.*\.(bpm|oom|eam|eab|oob)$' { 'Modèle natif PowerDesigner / échange'; break }
            '^02_Architecture\\.*\.(bpel|wsdl)$|^02_Architecture\\.*\\BPEL\\' { 'Contrat BPEL/WSDL de référence abstrait'; break }
            '^02_Architecture\\.*\.dmn$' { 'Modèle dʼexigences de décision DMN'; break }
            '^02_Architecture\\.*\.docx$|^02_Architecture\\.*\.pdf$|^02_Architecture\\.*\.html$' { 'Dossier dʼarchitecture ou note de modélisation'; break }
            '^03_Conception\\' { 'Source PlantUML éditable'; break }
            '^04_Tests-et-Validation\\Postman\\Preuves\\' { 'Capture originale Postman / lancement / santé; valeurs potentiellement sensibles'; break }
            '^04_Tests-et-Validation\\Postman\\Scripts\\.*\.json$' { 'Collection ou profil dʼenvironnement Postman'; break }
            '^04_Tests-et-Validation\\Postman\\Scripts\\.*\.ps1$' { 'Automatisation locale et lancement des services'; break }
            '^04_Tests-et-Validation\\Postman\\Rapports-historiques\.pdf$|^04_Tests-et-Validation\\Postman\\Rapports-historiques\\.*\.pdf$' { 'Ancien rapport visuel Postman archivé'; break }
            '^04_Tests-et-Validation\\Rapports-historiques\\Postman\\.*\.xml$' { 'Rapport JUnit historique de la collection Postman'; break }
            '^04_Tests-et-Validation\\Rapports-historiques\\.*\.html$' { 'Rapport HTML de tests archivé'; break }
            '^04_Tests-et-Validation\\fr\\rapport-tests-postman' { 'Rapport actuel des tests Postman en français'; break }
            '^04_Tests-et-Validation\\en\\' { 'OAuth contract test report in English'; break }
            '^05_Gestion-et-Livrables\\.*\.csv$' { 'Artefact de traçabilité ou registre des risques'; break }
            '^05_Gestion-et-Livrables\\fr\\' { 'Human-readable project management report in French'; break }
            '^05_Gestion-et-Livrables\\en\\' { 'Project management report in English'; break }
            '^06_Outils-de-Generation\\' { 'Outil de génération reproductible des rapports'; break }
            default { 'Fichier de référence, espace PowerDesigner ou notice de dossier' }
        }
        ,@($area, $relativePath, $extension, $role)
    }
    Add-Table $script:currentDocument @('Domaine', 'Chemin relatif', 'Format', 'Rôle du livrable') @($inventoryRows)
    Add-Paragraph $script:currentDocument "Nombre de livrables inventoriés : $($allFiles.Count). Les fichiers de verrouillage temporaires dʼéditeur sont exclus de lʼinventaire et ne sont pas supprimés."
    Add-Paragraph $script:currentDocument '3. Livrables projet référencés hors de documents\rapports' 'Heading 1'
    Add-Table $script:currentDocument @('Emplacement au dépôt', 'Contribution à la remise', 'Limite de classement') @(
        ,@('README.md et README des modules', 'Construction, configuration locale, limites fonctionnelles et commandes de test.', 'Restent à la racine et dans les modules Java; non déplacés dans les rapports.'),
        ,@('ci/jenkins/ et ci/concourse/', 'Pipelines reactor, tâches et JCasC isolés.', 'Configurations du dépôt, pas preuve dʼun serveur CI déployé ou dʼun pipeline exécuté.'),
        ,@('deployment/', 'Compose et fichiers de construction des conteneurs des deux services.', 'Déploiement de démonstration; lʼinfrastructure nʼest pas attestée en service.'),
        ,@('.run/', 'Configurations dʼexécution IDE.', 'Paramètres locaux, non destinés à remplacer une configuration dʼenvironnement.'),
        ,@('authorization-server/, resource-server/, integration-tests/', 'Code Java, tests unitaires/contrat et API OpenAPI.', 'La conformité dépend de la traçabilité et des écarts décrits dans les rapports.')
    )
    Add-Paragraph $script:currentDocument '4. Compléments de remise ajoutés' 'Heading 1'
    Add-Paragraph $script:currentDocument 'La matrice exigences-tests sépare les obligations textuelles du PDF des compléments; le registre des risques rend visibles les écarts de sécurité et de livraison. Les rapports Word couvrent les tests Postman, la faisabilité technique et lʼinventaire/classement. Ces artefacts complètent la remise sans prétendre remplacer les validations du commanditaire.'
    Add-Paragraph $script:currentDocument '5. Lacunes et points de contrôle avant acceptation' 'Heading 1'
    Add-Table $script:currentDocument @('Priorité', 'Point à livrer ou décider', 'Critère de clôture') @(
        ,@('Bloquant fonctionnel', 'Prise en charge dʼune nouvelle clé de signature par le Resource Server.', 'Test de contrat positif/negatif avec seconde clé et kid, ou dérogation écrite de lʼexigence.'),
        ,@('Bloquant preuve', 'Rapport Postman frais sur lʼenvironnement ciblé et version exacte des outils.', 'Run horodaté, XML exporté, URL/ports identifiés; aucun jeton/secret publié.'),
        ,@('Avant tout partage', 'Purification des captures et cycle de vie des jetons de démonstration.', 'Distribuer uniquement les copies expurgées; révoquer/invalider les jetons encore exploitables.'),
        ,@('Hors périmètre exercice', 'Gestion durable des clés/clients, TLS, supervision, sauvegardes, environnements réels.', 'Nouvelle décision de portée, responsable, budget, critères de sécurité et plan dʼexploitation.'),
        ,@('Documentation de gouvernance', 'Approbation des exigences, acceptation, revue de livraison et historique des décisions.', 'Nom/date/signature ou validation électronique du commanditaire; aucun accord nʼest inféré par ce rapport.')
    )
    Add-Paragraph $script:currentDocument '6. Limites des modèles et des rapports' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Les fichiers BPMN/UML/EAM et diagrammes sont des artefacts de conception. Les fichiers DMN et BPEL/WSDL décrivent des règles ou une orchestration de référence; ils ne prouvent pas lʼinstallation dʼun moteur DMN/BPEL. Les XML JUnit, HTML de tests et PDF Postman archivés conservent leurs dates dʼorigine. La présence dʼun dossier deployment ou CI ne vaut ni exécution en infrastructure ni certification.'
    Add-Paragraph $script:currentDocument '7. Références' 'Heading 1'
    Add-Paragraph $script:currentDocument 'Index du dossier : README.md. Spécification : 01_Cadrage\fr\. Architecture : 02_Architecture\fr et en\. Tests : 04_Tests-et-Validation\fr et en\. Traçabilité et risques : 05_Gestion-et-Livrables\.'
    $catalogueReport = Join-Path $ReportsRoot '05_Gestion-et-Livrables\fr\catalogue-livrables-ecarts.docx'
    Save-ReportDocument $script:currentDocument $catalogueReport
    $script:currentDocument = $null
}
finally {
    if ($script:currentDocument) {
        $script:currentDocument.Close(0)
    }
    if ($script:word) {
        $script:word.Quit()
        [System.Runtime.InteropServices.Marshal]::ReleaseComObject($script:word) | Out-Null
    }
    if (Test-Path -LiteralPath $workDirectory) {
        Remove-Item -LiteralPath $workDirectory -Recurse -Force
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
