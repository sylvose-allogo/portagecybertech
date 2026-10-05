# ============================================================================
# Organization : Portage Cybertech
# Project      : Mini OAuth 2.0 platform
# Purpose      : Automates creation or refresh of the configured PowerDesigner models and exported architecture diagrams; it does not itself create bilingual report exports.
# Author       : Sylvose Allogo <sylvose.allogo@yahoo.com>
# Version      : 1.0.0
# Date         : 2026-10-04
# ============================================================================
param(
    [string] $OutputDirectory = $PSScriptRoot + '\..',
    [string] $PowerDesignerHome = 'C:\Program Files (x86)\Sybase\PowerDesigner 16'
)

$ErrorActionPreference = 'Stop'
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
$constantsPath = Join-Path $PowerDesignerHome 'Ole Automation\VBScriptConstants.vbs'
$bpmnTemplatePath = Join-Path $PowerDesignerHome 'Examples\BPMN20.bpm'

foreach ($requiredPath in @($constantsPath, $bpmnTemplatePath)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Required PowerDesigner 16 file not found: $requiredPath"
    }
}

$constantsText = [System.IO.File]::ReadAllText($constantsPath)
function Get-PowerDesignerClassId {
    param([string] $ClassName)
    $pattern = "(?m)^const\s+$([regex]::Escape($ClassName))\s*=\s*(-?\d+)"
    $match = [regex]::Match($constantsText, $pattern)
    if (-not $match.Success) {
        throw "PowerDesigner class identifier '$ClassName' was not found in $constantsPath."
    }
    return [int]$match.Groups[1].Value
}

function Set-ModelMetadata {
    param($Model, [string] $Name, [string] $Code, [string] $Description)
    $Model.Name = $Name
    $Model.Code = $Code
    $Model.Author = 'Portage CyberTech'
    $Model.Description = $Description
}

function Add-ModelElement {
    param($Model, [int] $ClassId, [string] $Name, [string] $Description)
    $element = $Model.CreateObject($ClassId)
    $element.Name = $Name
    $element.Code = $Name -replace '[^A-Za-z0-9_]', '_'
    if ($element.HasAttribute('Description')) {
        $element.Description = $Description
    }
    return $element
}

function Save-ModelAndImage {
    param($Model, $Diagram, [string] $ModelPath, [string] $ImagePath)
    $Diagram.CompleteLinks()
    $Diagram.AutoLayout()
    $Model.Save($ModelPath)
    $Diagram.ExportImage($ImagePath)
    foreach ($path in @($ModelPath, $ImagePath)) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf) -or (Get-Item -LiteralPath $path).Length -lt 1000) {
            throw "PowerDesigner did not create the expected model or diagram image: $path"
        }
    }
}

New-Item -ItemType Directory -Force -Path `
    (Join-Path $OutputDirectory 'bpmn'), `
    (Join-Path $OutputDirectory 'uml'), `
    (Join-Path $OutputDirectory 'eam'), `
    (Join-Path $OutputDirectory 'diagrams') | Out-Null

$application = New-Object -ComObject PowerDesigner.Application
$application.InteractiveMode = $false
$application.ValidationMode = $false

$bpmnProfileModel = $application.OpenModel($bpmnTemplatePath)
if ($bpmnProfileModel.ProcessLanguage.Code -ne 'BPMN2') {
    throw "The installed BPMN template does not expose the expected BPMN2 process language."
}
$bpmn = $application.CreateModel((Get-PowerDesignerClassId 'PdBPM_Model'), '|Diagram=BusinessProcessDiagram')
$bpmn.ProcessLanguage = $bpmnProfileModel.ProcessLanguage
Set-ModelMetadata $bpmn `
    'Portage CyberTech OAuth 2.0 Process' `
    'PortageCyberTechOAuth2Process' `
    'BPMN 2.0 view of subject-token issue/exchange and protected API access. The Resource Server additional-key validation is a target requirement; the corresponding end-to-end behaviour is not currently demonstrated by the Java repository.'
$bpmnDiagram = $bpmn.BusinessProcessDiagrams.Item(0)
$bpmnDiagram.Name = 'Token issue, exchange and protected resource'

$processNodes = @{}
$processNodes.Start = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_ProcessStart') `
    'Client request received' 'Client submits a subject-grant or a trusted RFC 8693 token-exchange request.'
$processNodes.Request = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Process') `
    'POST /oauth2/token' 'The client authenticates to the token endpoint and submits the selected grant.'
$processNodes.Grant = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Decision') `
    'Which token grant?' 'The subject grant is a local simulation; the caller-supplied subject does not authenticate a user.'
$processNodes.Source = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Process') `
    'Validate RFC 8693 source JWT' 'Validate the configured source-token signature, issuer, expiry, audience and scopes.'
$processNodes.SourceDecision = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Decision') `
    'Is the source JWT trusted?' 'Reject an invalid source token using the OAuth invalid_grant response.'
$processNodes.Issue = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Process') `
    'Sign five-minute RS256 access JWT' 'Limit target audience and scopes before issuing the new signed token.'
$processNodes.Receive = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Process') `
    'Return the access token' 'Return the issued JWT to the OAuth client.'
$processNodes.Call = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Process') `
    'GET /api/hello with Bearer token' 'Client calls the protected resource using the issued access token.'
$processNodes.Verify = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Decision') `
    'Does the Resource Server trust the JWT?' 'Target acceptance includes a separately configured additional public key; that request-path behaviour remains open in the repository.'
$processNodes.Scope = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Decision') `
    'Does the token contain api.read?' 'A valid token without the required authority receives HTTP 403.'
$processNodes.Hello = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Process') `
    'Return HelloWorld (HTTP 200)' 'Return the protected greeting for the validated JWT subject.'
$processNodes.Reject401 = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_ProcessEnd') `
    'Reject untrusted token (HTTP 401)' 'Do not disclose protected content.'
$processNodes.Reject403 = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_ProcessEnd') `
    'Reject insufficient scope (HTTP 403)' 'Authentication succeeded, but the required API authority is absent.'
$processNodes.RejectGrant = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_ProcessEnd') `
    'Reject invalid source token (invalid_grant)' 'Token-exchange source validation failed.'
$processNodes.End = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_ProcessEnd') `
    'Request completed successfully' 'Protected resource returned successfully.'

$bpmnFlows = @(
    @('Start', 'Request', 'HTTP POST'),
    @('Request', 'Grant', 'authenticated OAuth client'),
    @('Grant', 'Issue', 'subject grant; isolated demo only'),
    @('Grant', 'Source', 'RFC 8693 token exchange'),
    @('Source', 'SourceDecision', 'validated source claims'),
    @('SourceDecision', 'Issue', 'yes'),
    @('SourceDecision', 'RejectGrant', 'no'),
    @('Issue', 'Receive', 'signed access JWT'),
    @('Receive', 'Call', 'Bearer token'),
    @('Call', 'Verify', 'HTTP request'),
    @('Verify', 'Scope', 'trusted signature and claims'),
    @('Verify', 'Reject401', 'no'),
    @('Scope', 'Hello', 'yes: api.read'),
    @('Scope', 'Reject403', 'no: api.read absent'),
    @('Hello', 'End', 'HTTP 200')
)
foreach ($item in $processNodes.Values) {
    $bpmnDiagram.AttachObject($item) | Out-Null
}
foreach ($item in $bpmnFlows) {
    $flow = Add-ModelElement $bpmn (Get-PowerDesignerClassId 'PdBPM_Flow') $item[2] 'Sequence flow in the OAuth 2.0 access process.'
    $flow.Source = $processNodes[$item[0]]
    $flow.Destination = $processNodes[$item[1]]
}

$bpmnPath = Join-Path $OutputDirectory 'bpmn\PortageCyberTech-OAuth2-Process.bpm'
$bpmnImage = Join-Path $OutputDirectory 'diagrams\PowerDesigner-BPMN-Process.png'
Save-ModelAndImage $bpmn $bpmnDiagram $bpmnPath $bpmnImage
Write-Output "BPMN 2.0 saved: $bpmnPath ($($bpmn.Processes.Count) process activities, $($bpmn.ProcessDecisions.Count) decisions)."

$oom = $application.CreateModel((Get-PowerDesignerClassId 'PdOOM_Model'), '|Language=Java|Diagram=UseCaseDiagram')
Set-ModelMetadata $oom `
    'Portage CyberTech OAuth 2.0 Application Design' `
    'PortageCyberTechOAuth2ApplicationDesign' `
    'UML 2.5 logical use-case and component views; the integration test module consumes the production services over HTTP in test scope only.'
$useCaseDiagram = $oom.UseCaseDiagrams.Item(0)
$useCaseDiagram.Name = 'OAuth client and protected API use cases'
$clientActor = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_Actor') 'OAuth client' 'HTTP client, application or Postman collection.'
$operatorActor = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_Actor') 'Test operator' 'Runs the local demonstration and cross-service contract suite.'
$useIssue = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_UseCase') 'Request or exchange an access JWT' 'Client requests a signed token through the configured grant.'
$useResource = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_UseCase') 'Read the protected HelloWorld resource' 'Client presents a Bearer JWT to GET /api/hello.'
$useAdditionalKey = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_UseCase') 'Verify a JWT with the additional test key' 'Acceptance target from the source brief; Resource Server end-to-end implementation remains open.'
$useCaseObjects = @($clientActor, $operatorActor, $useIssue, $useResource, $useAdditionalKey)
foreach ($item in $useCaseObjects) {
    $useCaseDiagram.AttachObject($item) | Out-Null
}
foreach ($association in @(
    @($clientActor, $useIssue),
    @($clientActor, $useResource),
    @($operatorActor, $useAdditionalKey)
)) {
    $link = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_UseCaseAssociation') 'Association' 'Use-case participation.'
    $link.Object1 = $association[0]
    $link.Object2 = $association[1]
    $useCaseDiagram.AttachLinkObject($link) | Out-Null
}

$componentDiagram = $oom.CreateObject((Get-PowerDesignerClassId 'PdOOM_ComponentDiagram'))
$componentDiagram.Name = 'Deployable service and test components'
$componentAuthorization = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_Component') 'authorization-server' 'Independent Spring Boot OAuth token and JWKS service.'
$componentResource = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_Component') 'resource-server' 'Independent Spring Boot API; no production dependency on authorization-server Java code.'
$componentTests = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_Component') 'integration-tests' 'Test-scope HTTP contract tests for both Spring Boot applications.'
$componentJwks = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_Interface') 'JWT and public JWKS HTTP contracts' 'Cross-service trust uses Bearer JWT and public keys, not a Java module dependency.'
foreach ($item in @($componentAuthorization, $componentResource, $componentTests, $componentJwks)) {
    $componentDiagram.AttachObject($item) | Out-Null
}
foreach ($dependency in @(
    @($componentResource, $componentJwks),
    @($componentTests, $componentAuthorization),
    @($componentTests, $componentResource)
)) {
    $link = Add-ModelElement $oom (Get-PowerDesignerClassId 'PdOOM_Dependency') 'HTTP contract dependency' 'Network-level dependency; test links apply to test scope only.'
    $link.Object1 = $dependency[0]
    $link.Object2 = $dependency[1]
    $componentDiagram.AttachLinkObject($link) | Out-Null
}

$oomPath = Join-Path $OutputDirectory 'uml\PortageCyberTech-Use-Cases-and-Components.oom'
$oomUseCaseImage = Join-Path $OutputDirectory 'diagrams\PowerDesigner-UML-Use-Cases.png'
$oomComponentImage = Join-Path $OutputDirectory 'diagrams\PowerDesigner-UML-Components.png'
Save-ModelAndImage $oom $useCaseDiagram $oomPath $oomUseCaseImage
$oom.Save($oomPath)
$componentDiagram.CompleteLinks()
$componentDiagram.AutoLayout()
$componentDiagram.ExportImage($oomComponentImage)
if (-not (Test-Path -LiteralPath $oomComponentImage -PathType Leaf) -or (Get-Item -LiteralPath $oomComponentImage).Length -lt 1000) {
    throw "PowerDesigner did not export the UML component diagram: $oomComponentImage"
}
Write-Output "UML OOM saved: $oomPath ($($oom.UseCases.Count) use cases, $($oom.Components.Count) components, $($oom.ComponentDiagrams.Count) component diagram)."

$eam = $application.CreateModel((Get-PowerDesignerClassId 'PdEAM_Model'), '|Diagram=ApplicationArchitectureDiagram')
Set-ModelMetadata $eam `
    'Portage CyberTech Application and Physical Architecture' `
    'PortageCyberTechApplicationAndPhysicalArchitecture' `
    'EAM application-context and local physical-topology views. Production network, identity, database and infrastructure choices are not asserted by this model.'
$applicationDiagram = $eam.ApplicationArchitectureDiagrams.Item(0)
$applicationDiagram.Name = 'Application architecture — OAuth services and HTTP contracts'
$eamAuthorization = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_EnterpriseApplication') 'Authorization Server' 'Spring Boot service: POST /oauth2/token and GET /oauth2/jwks.'
$eamResource = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_EnterpriseApplication') 'Resource Server' 'Spring Boot service: protected GET /api/hello.'
$eamClient = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_EnterpriseApplication') 'OAuth client / Postman' 'Obtains and presents JWT access tokens.'
$eamTests = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_EnterpriseApplication') 'Integration tests (test scope)' 'Exercises HTTP, JWT and JWKS contracts without production Java coupling.'
$eamTokenService = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_ApplicationService') 'OAuth token and JWKS contract' 'Client authentication, token issue/exchange and public JWKS publication.'
$eamHelloService = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_ApplicationService') 'Protected HelloWorld API contract' 'Bearer JWT validation, issuer/audience/scope policy and API response.'
foreach ($item in @($eamAuthorization, $eamResource, $eamClient, $eamTests, $eamTokenService, $eamHelloService)) {
    $applicationDiagram.AttachObject($item) | Out-Null
}
foreach ($relationship in @(
    @($eamAuthorization, $eamTokenService),
    @($eamResource, $eamHelloService),
    @($eamClient, $eamTokenService),
    @($eamClient, $eamHelloService),
    @($eamTests, $eamTokenService),
    @($eamTests, $eamHelloService)
)) {
    $link = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_ApplicationLink') 'uses HTTP contract' 'Logical application relationship; tests are test-scope only.'
    $link.Object1 = $relationship[0]
    $link.Object2 = $relationship[1]
    $applicationDiagram.AttachLinkObject($link) | Out-Null
}

$technologyDiagram = $eam.CreateObject((Get-PowerDesignerClassId 'PdEAM_TechnologyInfrastructureDiagram'))
$technologyDiagram.Name = 'Observed localhost processes — not a production topology'
$localHost = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_HardwareServer') 'Local developer host' 'Observed localhost workstation for the demo and contract tests; not a cloud or production deployment.'
$localHost.Type = 'Development host'
$authorizationProcess = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_SoftwareServer') 'Authorization :9090' 'Authorization Server Spring Boot process; ephemeral RSA key and in-memory OAuth client registry in demo configuration.'
$authorizationProcess.Type = 'Spring Boot'
$resourceProcess = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_SoftwareServer') 'Resource API :8080' 'Resource Server Spring Boot process; trusts configured issuer, audience and public JWKS.'
$resourceProcess.Type = 'Spring Boot'
$testRunner = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_SoftwareServer') 'Maven tests' 'Maven integration-test process starts both services for local HTTP contract tests.'
$testRunner.Type = 'Test runner'
foreach ($item in @($localHost, $authorizationProcess, $resourceProcess, $testRunner)) {
    $technologyDiagram.AttachObject($item) | Out-Null
}
$physicalRelationships = @(
    @($localHost, $authorizationProcess),
    @($localHost, $resourceProcess),
    @($localHost, $testRunner)
)
foreach ($relationship in $physicalRelationships) {
    $link = Add-ModelElement $eam (Get-PowerDesignerClassId 'PdEAM_InfrastructureLink') 'local process on host' 'Observed conceptual localhost relationship for development and tests.'
    $link.Object1 = $relationship[0]
    $link.Object2 = $relationship[1]
    $link.Type = 'local'
    $technologyDiagram.AttachLinkObject($link) | Out-Null
}

$eamPath = Join-Path $OutputDirectory 'eam\PortageCyberTech-Application-and-Physical-Architecture.eam'
$eamApplicationImage = Join-Path $OutputDirectory 'diagrams\PowerDesigner-EAM-Application-Architecture.png'
$eamPhysicalImage = Join-Path $OutputDirectory 'diagrams\PowerDesigner-EAM-Physical-Architecture.png'
Save-ModelAndImage $eam $applicationDiagram $eamPath $eamApplicationImage
$eam.Save($eamPath)
$technologyDiagram.CompleteLinks()
$technologyDiagram.AutoLayout()
$technologyDiagram.ExportImage($eamPhysicalImage)
if (-not (Test-Path -LiteralPath $eamPhysicalImage -PathType Leaf) -or (Get-Item -LiteralPath $eamPhysicalImage).Length -lt 1000) {
    throw "PowerDesigner did not export the EAM physical diagram: $eamPhysicalImage"
}
Write-Output "EAM EAM saved: $eamPath ($($eam.EnterpriseApplications.Count) applications, $($eam.SoftwareServers.Count) local processes, 2 architecture diagrams)."

$expectedModels = @(
    @{ Path = $bpmnPath; Type = 'Business Process Model' },
    @{ Path = $oomPath; Type = 'Object-Oriented Model' },
    @{ Path = $eamPath; Type = 'Enterprise Architecture Model' }
)
foreach ($expected in $expectedModels) {
    $openedModel = $application.OpenModel($expected.Path)
    if ($openedModel.ClassName -ne $expected.Type) {
        throw "Unexpected PowerDesigner model type in $($expected.Path): $($openedModel.ClassName)"
    }
    Write-Output "Reopened native model: $($openedModel.Name) [$($openedModel.ClassName)]."
}
