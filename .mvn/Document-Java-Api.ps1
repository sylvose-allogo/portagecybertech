# ============================================================================
# Organization : Portage Cybertech
# Project      : Mini OAuth 2.0 platform
# Purpose      : Scans production, test, and nested Java sources, then generates class/method JavaDoc with project attribution, parameter contracts, returns, and checked-exception summaries.
# Author       : Sylvose Allogo <sylvose.allogo@yahoo.com>
# Version      : 1.0.0
# Date         : 2026-10-04
# ============================================================================
[CmdletBinding()]
param(
    [string] $ProjectRoot = '',
    [string] $ReleaseVersion = '1.0.0',
    [string] $DocumentationDate = '2026-10-04'
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $scriptPath = if ([string]::IsNullOrWhiteSpace($PSScriptRoot)) {
        $MyInvocation.MyCommand.Path
    } else {
        Join-Path $PSScriptRoot 'Document-Java-Api.ps1'
    }
    $ProjectRoot = Split-Path -Parent (Split-Path -Parent $scriptPath)
}
$ProjectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
$author = 'Sylvose Allogo <sylvose.allogo@yahoo.com>'
$descriptions = @{
    AuthorizationServerApplication = 'Bootstraps the OAuth 2.0 Authorization Server Spring Boot service. It assembles the authorization endpoints, signing-key publication, application configuration, and service lifecycle; it does not itself authenticate an end user.'
    SecurityConfiguration = 'Defines the Authorization Server security boundary and its Spring beans: ephemeral RSA signing material, public JWKS publication, JWT encoding and validation, client registration, grant-specific token generation, and ordered HTTP filter chains. Runtime secrets and production-grade persistent key management must be supplied separately.'
    AccessTokenIssuer = 'Builds, signs, records, and returns short-lived OAuth access tokens for the configured grant providers. It binds the registered client, authenticated client principal, subject principal, allowed scopes, requested audience, grant type, and grant request into Spring Authorization Server token and authorization objects.'
    OAuth2GrantRequestParameters = 'Copies non-grant-type form parameters from an HTTP token request into the parameter map consumed by custom OAuth grant authentication tokens. It preserves the first value of each submitted parameter and omits empty values.'
    OAuth2GrantRequestSupport = 'Centralizes validation shared by custom grants: authenticated OAuth client enforcement, requested-scope subset checking, and requested-audience allow-list enforcement. It raises OAuth errors rather than silently accepting invalid grant input.'
    SubjectGrantAuthenticationConverter = 'Recognizes the local demonstration subject grant on the token endpoint and converts its form parameters and current authenticated client into a Spring Security authentication request. This grant trusts a caller-supplied subject and is not an end-user authentication mechanism.'
    SubjectGrantAuthenticationProvider = 'Authenticates and authorizes requests for the local subject grant. It checks the authenticated and registered client, validates the caller-supplied subject and scopes, then delegates JWT creation to the shared access-token issuer; enable this grant only in an isolated demonstration.'
    SubjectGrantAuthenticationToken = 'Represents an unprocessed request for the local caller-supplied-subject OAuth grant. It carries the authenticated client principal and grant form parameters to the corresponding authentication provider.'
    TokenExchangeAuthenticationConverter = 'Recognizes RFC 8693 token-exchange requests on the token endpoint and converts their form fields and current authenticated client into a Spring Security grant authentication token.'
    TokenExchangeAuthenticationProvider = 'Implements the RFC 8693 access-token exchange contract. It validates the client, source-token type and signature/claims, intersects requested scopes with source and client permissions, enforces the audience allow-list, and delegates new-token issuance.'
    TokenExchangeAuthenticationToken = 'Represents an unprocessed RFC 8693 token-exchange request, including the authenticated client and submitted subject-token parameters.'
    ResourceServerApplication = 'Bootstraps the protected Resource Server Spring Boot service and its application lifecycle. HTTP authorization, token decoding, and endpoint behaviour are configured by dedicated components.'
    PortageCyberTechController = 'Exposes the protected Hello World resource API. The endpoint uses the authenticated JWT principal as the greeting subject and relies on the Resource Server security chain to enforce bearer-token validity and the api.read authority.'
    ResourceSecurityConfiguration = 'Configures JWT trust for the Resource Server and the ordered HTTP security chains. API requests require a JWT whose issuer and audience match configuration and whose authorities include SCOPE_api.read; health and API-documentation routes remain separately accessible.'
    JwtTokenDecoder = 'Provides validated access-token inspection utilities. Spring Security performs the configured JWT validation; Nimbus JOSE/JWT is used to inspect the compact token, retrieve the matching public RSA key from JWKS, and independently verify the RS256 signature.'
    DecodedToken = 'Immutable view of a successfully decoded JWT containing its JOSE header, validated claims, and encoded signature.'
    JwtTokenValidation = 'Coordinates access-token checks and returns a structured validation report containing selected JWT metadata. Its boolean helpers convert expected token, configuration, or network validation failures into a negative result for demonstration and tests.'
    ValidatedToken = 'Immutable report of validated JWT metadata: decoded header, JSON claims, bearer-header form, expiration, signing algorithm, signature result, selected RSA public key, and audiences.'
    CucumberSpringConfiguration = 'Connects the Cucumber integration-test engine to the Authorization Server Spring test context and MockMvc. It supplies a reusable Spring context for HTTP-level acceptance steps.'
    CucumberTest = 'JUnit Platform suite entry point that discovers the Cucumber feature resources and binds them to the project integration-test step definitions.'
    MiniPlateformeOAuthSteps = 'Implements Cucumber acceptance steps for authenticated subject-grant token issuance, anonymous-client rejection, public JWKS retrieval, and response assertions against the Authorization Server contract.'
    MicroservicesTest = 'Starts the Authorization Server and Resource Server as separate local Spring Boot processes and exercises their HTTP/JWT/JWKS contracts end to end, including valid access, tampered signatures, and public-key discovery.'
    HttpClientTestConfiguration = 'Provides the Java HTTP client used by the integration tests to call the separately running local microservices.'
    SecurityConfigurationTest = 'Unit and Spring-context tests for Authorization Server beans and security policy: key publication, client grants and credentials, audience and issuer validation, token lifetime, and token generator wiring.'
    DeploymentSmokeTest = 'Smoke tests for the deployed Authorization Server HTTP surface, checking health, availability of the OpenAPI document, and public-only JWKS publication.'
    JwksEndPointTest = 'Contract tests for the public JWKS endpoint, including unauthenticated access, JSON media type, correspondence to the active RSA signing key, and absence of private key parameters.'
    JwtEndPointTest = 'Contract tests for OAuth access-token issuance, including a valid local subject-grant request and rejection of missing or invalid client credentials and subject input.'
    AuthorizationServerMicroserviceTest = 'HTTP contract tests for the running Authorization Server, verifying JWKS exposure, signed JWT claims and signatures, client authentication, and scope rejection.'
    OAuth2GrantRequestSupportTest = 'Focused tests for default audience selection and rejection of requested or configured audiences that fall outside the configured allow-list.'
    AdditionalSigningKeyTest = 'Tests the Authorization Server decoder configuration with an additional public key identified by kid. This verifies source-token decoding at the Authorization Server and is not a Resource Server additional-key end-to-end test.'
    OAuthSystemTest = 'System-level tests for the Authorization Server OAuth flow: local source-token creation, RFC 8693 exchange, output claims and JWKS signature verification, plus rejection of untrusted audiences, insufficient scopes, and tampered source tokens.'
    SubjectGrantAuthenticationProviderTest = 'Unit tests for subject-grant handling, verifying that an authenticated registered client and allowed subject/scope cause delegation to the shared token issuer.'
    JwtTokenDecoderTest = 'Unit tests for validated JWT inspection, bearer formatting, claims and audience extraction, expiration handling, JWKS key lookup, and RS256 signature verification.'
    PortageCyberTechControllerTest = 'HTTP-level Resource Server controller tests covering valid JWT access, JWKS-backed signatures, required scopes, anonymous or malformed bearer rejection, and the health endpoint.'
    ResourceServerMicroserviceTest = 'Starts the Resource Server with a local test JWKS endpoint and verifies its HTTP contract for valid signatures, invalid signatures, required scopes, and missing bearer tokens.'
    SeleniumSmokeTest = 'Optional browser-oriented smoke tests for the demonstration UI and protected-resource flow. Some scenarios are disabled or require locally installed browser-driver tooling and external connectivity.'
    JwtTokenValidationTest = 'Unit tests for validation-report construction, invalid and expired JWT handling, boolean validation results, and verification through a local JWKS endpoint.'
    StubTokenDecoder = 'Test-only JwtTokenDecoder substitute that returns controlled metadata or validation failures so validation-service behaviour can be tested without network calls.'
    TestRsaPublicKey = 'Minimal test RSA public-key value used to verify that validated-token reports preserve the selected public-key reference.'
    ProtectReportPdfs = 'Command-line PDF protection utility used by the integration-test module to scan report trees, apply AES-256 encryption and restrictive permissions, verify the saved output, preserve timestamps, and safely replace each file. It must receive an explicit reports root and a non-empty opening password on standard input.'
    PdfProtectionTestSupport = 'Shared test fixture for creating temporary PDF inputs, invoking the packaged PowerShell protection runner, and verifying password enforcement and every configured PDF permission without touching repository reports.'
    ProtectReportPdfsUnitTest = 'Focused utility-level demonstration test for a single temporary PDF, invoking the actual PowerShell/PDFBox protection workflow and asserting password and permission enforcement.'
    ProtectReportPdfsIntegrationTest = 'Integration test for recursive discovery and protection of PDF files located in multiple nested report-language directories.'
    ProtectReportPdfsSystemTest = 'System-level test of complete report-tree processing, including root and nested PDFs and preservation of a non-PDF companion file.'
    ProtectReportPdfsMicroserviceTest = 'Microservice-oriented test verifying the shared report-protection workflow against separate Authorization Server and Resource Server report directories.'
}

function Split-ParameterList {
    param([string] $ParameterText)

    $parts = New-Object 'System.Collections.Generic.List[string]'
    $start = 0
    $angleDepth = 0
    $parenDepth = 0
    $bracketDepth = 0
    for ($index = 0; $index -lt $ParameterText.Length; $index++) {
        switch ($ParameterText[$index]) {
            '<' { $angleDepth++ }
            '>' { if ($angleDepth -gt 0) { $angleDepth-- } }
            '(' { $parenDepth++ }
            ')' { if ($parenDepth -gt 0) { $parenDepth-- } }
            '[' { $bracketDepth++ }
            ']' { if ($bracketDepth -gt 0) { $bracketDepth-- } }
            ',' {
                if ($angleDepth -eq 0 -and $parenDepth -eq 0 -and $bracketDepth -eq 0) {
                    $parts.Add($ParameterText.Substring($start, $index - $start).Trim())
                    $start = $index + 1
                }
            }
        }
    }
    $last = $ParameterText.Substring($start).Trim()
    if ($last.Length -gt 0) {
        $parts.Add($last)
    }
    return ,$parts.ToArray()
}

function Get-ParameterName {
    param([string] $Parameter)
    $withoutAnnotations = [regex]::Replace($Parameter, '@[\w.]+(?:\s*\([^)]*\))?', ' ')
    $withoutModifiers = [regex]::Replace($withoutAnnotations, '\bfinal\b', ' ')
    $identifiers = [regex]::Matches($withoutModifiers, '[A-Za-z_$][\w$]*')
    if ($identifiers.Count -lt 2) {
        throw "Could not identify a parameter name in '$Parameter'."
    }
    return $identifiers[$identifiers.Count - 1].Value
}

function Get-ParameterDescription {
    param([string] $Name)
    $descriptionsByName = @{
        accessToken = 'Compact JWT to inspect or validate; an Authorization header value may include a leading standard Bearer prefix.'
        token = 'Compact JWT or token value being processed by the current operation.'
        subject = 'Subject identifier represented in the issued token; the demonstration grant does not authenticate this value.'
        subjectPrincipal = 'Authentication principal whose subject and authorities are used when generating the access token.'
        clientPrincipal = 'Authenticated OAuth client principal associated with this grant request.'
        authentication = 'Spring Security authentication object supplied to the provider for validation and processing.'
        request = 'Incoming HTTP request whose grant type and form parameters are inspected.'
        args = 'Command-line arguments; ProtectReportPdfs requires exactly one argument containing the existing reports-root directory.'
        apiAudience = 'Expected or default API audience URI embedded in, or validated against, the JWT.'
        audience = 'Requested resource audience; it must be present in the configured allow-list.'
        defaultAudience = 'Audience selected when the request does not explicitly provide one.'
        allowedAudiences = 'Configured set of permitted audience URIs.'
        allowedScopes = 'Scopes registered as available to the OAuth client.'
        requestedScopes = 'Scopes requested for the issued or exchanged access token.'
        rawScope = 'Unparsed scope form parameter; whitespace-separated values are checked against permitted scopes.'
        rawAudience = 'Unparsed audience form parameter submitted to the token endpoint.'
        jwksUri = 'URI of the JSON Web Key Set used to resolve and verify public signing keys.'
        issuer = 'Expected JWT issuer identifier.'
        subjectToken = 'Source JWT submitted for RFC 8693 token exchange.'
        subjectTokenType = 'OAuth token-type identifier describing the submitted source token.'
        requestedTokenType = 'Optional OAuth token-type identifier requested for the exchange result.'
        registry = 'Spring dynamic-property registry populated with local test service settings.'
        expectedStatus = 'Expected HTTP status code asserted by the Cucumber step.'
        signingKey = 'RSA signing key or key pair used to produce the JWT under test.'
        signingPair = 'Key pair whose private key signs the JWT and whose public key is configured for verification.'
        keyPair = 'RSA key pair used to sign or validate a test JWT.'
        keyId = 'JWT key identifier (kid) used to select the matching public JWKS key.'
        kid = 'JWT key identifier (kid) used to select the matching public JWKS key.'
        scope = 'Space-delimited OAuth scope requested for the token or encoded in the JWT.'
        path = 'Relative HTTP endpoint path appended to the local service base URL.'
        result = 'HTTP exchange result whose response body contains the token or JSON document.'
        response = 'HTTP response or payload produced by the operation under test.'
        decoder = 'JWT decoder configured for the trust policy being exercised.'
        jwtDecoder = 'Spring JWT decoder used to validate signature, issuer, audience, and standard claims.'
        tokenDecoder = 'Collaborator that decodes JWTs and retrieves their validated metadata.'
        client = 'Registered OAuth client whose grants, scopes, and credentials govern token issuance.'
        clients = 'Repository used to locate a registered OAuth client by its client identifier.'
        encoder = 'Password encoder used to hash and verify registered OAuth client secrets.'
        tokenIssuer = 'Shared component that signs, persists, and returns OAuth access tokens.'
        grant = 'Custom OAuth grant authentication request being validated and processed.'
        grantType = 'OAuth grant type associated with the token request.'
        grantAuthentication = 'Authentication token containing the custom grant request and its parameters.'
        scopes = 'Authorized scopes included in the resulting access token.'
        privateParameter = 'Private JWK parameter name that must not be exposed by the public JWKS.'
        audienceReadFails = 'Whether the test double simulates a failure while reading JWT audiences.'
        signatureValid = 'Expected signature-validation result supplied to the test double.'
        expiration = 'JWT expiration instant used to test time-based validation.'
        publicKey = 'RSA public key expected to verify the token signature.'
        signingAlgorithm = 'JWS signing-algorithm name expected in the validated token report.'
        bearerAuthorizationValue = 'Expected Authorization header value formatted with the standard Bearer authorization scheme.'
        claimsJson = 'JSON representation of the validated JWT claims returned by the test double.'
        audiences = 'Audience URIs expected in the validated-token result.'
        jwksServer = 'Local HTTP server serving the public test key set.'
        httpClient = 'HTTP client used to call the independently running services.'
        objectMapper = 'Jackson mapper used to parse and serialize JSON contract payloads.'
        mvc = 'Spring MockMvc client used to exercise HTTP routes in-process.'
        header = 'JOSE header extracted from the compact JWT.'
        restClientBuilder = 'Spring RestClient builder used to create the HTTP client for JWKS retrieval.'
        claims = 'Validated JWT claim set represented as name/value pairs.'
        signature = 'Encoded JWS signature component extracted from the compact token.'
        jwtHeader = 'Decoded JOSE header returned as a map of header names and values.'
        secret = 'OAuth client secret supplied for credential verification.'
        jwtCustomizer = 'JWT customizer that adds project-specific claims to generated access tokens.'
        subjectProvider = 'Provider that resolves the subject principal represented by the issued token.'
        jwkSource = 'JWK source containing signing material and exposing the corresponding public key.'
        jwtEncoder = 'Nimbus encoder that signs JWTs using the configured JWK source.'
        authorizationService = 'Authorization service that stores issued token and grant metadata.'
        sub = 'JWT subject claim identifying the principal represented by the token.'
        exchangeProvider = 'Authentication provider that validates and processes token-exchange requests.'
        tokenGenerator = 'OAuth token generator that creates and signs access tokens.'
        http = 'HTTP client or server handle used by this operation.'
        expiresAt = 'JWT expiration instant used to determine whether the token remains valid.'
        signingPublicKey = 'RSA public key corresponding to the signing key used to verify the token.'
        parameters = 'Map of submitted OAuth grant parameters passed to the authentication request.'
        value = 'Value being encoded, decoded, or asserted by the operation.'
        reportsRoot = 'Existing directory tree whose PDF files are in scope for protection; the utility traverses its descendants recursively.'
        pdf = 'PDF file whose encryption state and user permissions are verified.'
        password = 'Non-empty test-only or operator-provided PDF opening password; never log or persist this value.'
        shell = 'PowerShell executable used to launch the report-protection resource script.'
        executable = 'PowerShell executable name checked before starting the protection test.'
        userPassword = 'Non-empty PDF user/opening password supplied interactively or through the secure test parameter; it must never be logged or written to a file.'
        document = 'PDFBox document currently being inspected or protected.'
        random = 'Cryptographically secure random-number generator used to create the independent PDF owner password.'
        temporary = 'Temporary output PDF written and verified before replacing the original report file.'
        protectedDocument = 'Reopened temporary PDF used to verify encryption and permission flags before replacing the source.'
    }
    if ($descriptionsByName.ContainsKey($Name)) {
        return $descriptionsByName[$Name]
    }
    $words = [regex]::Replace($Name, '([a-z0-9])([A-Z])', '$1 $2')
    return "Parameter used by this operation; its expected form and validation are described in the method contract."
}

function Get-MethodDescription {
    param([string] $Name, [string] $ClassName, [string] $ReturnType, [int] $ParameterCount)
    $methodDescriptions = @{
        'AuthorizationServerApplication.main' = 'Starts the Authorization Server Spring Boot application and delegates process configuration and shutdown handling to SpringApplication.'
        'SecurityConfiguration.signingKey' = 'Generates a fresh 2048-bit RSA key pair and wraps it in a signing JWK with a unique key identifier. The key is held in memory and is not a persistent production key-management strategy.'
        'SecurityConfiguration.jwkSource' = 'Publishes the configured signing JWK through a selector-backed Nimbus JWK source so the token generator can sign JWTs and the authorization endpoint can expose the corresponding public key.'
        'SecurityConfiguration.jwtEncoder' = 'Creates the Nimbus JWT encoder backed by the configured JWK source.'
        'SecurityConfiguration.jwtCustomizer' = 'Adds the configured default API audience to generated JWT claims, or uses an audience explicitly provided by a validated token grant.'
        'SecurityConfiguration.tokenGenerator' = 'Creates the JWT token generator and registers the project audience customizer.'
        'SecurityConfiguration.jwtDecoder' = 'Builds a source-token decoder using a remote JWKS URI when configured, otherwise the local or configured primary RSA public key, and optionally dispatches to a second key by JWT kid. Every route enforces the configured issuer and API audience.'
        'SecurityConfiguration.decoder' = 'Creates an RSA public-key JWT decoder and attaches the shared issuer and audience validators.'
        'SecurityConfiguration.configureValidators' = 'Combines Spring Security default issuer/time validators with a required API-audience validator on the supplied Nimbus decoder.'
        'SecurityConfiguration.passwordEncoder' = 'Creates Spring Security''s delegating password encoder for registered OAuth client secrets.'
        'SecurityConfiguration.registeredClientRepository' = 'Registers the configured confidential agent client, client-secret authentication, permitted API scope, RFC 8693 token exchange grant, optional local subject grant, and five-minute access-token lifetime in an in-memory repository.'
        'SecurityConfiguration.authorizationService' = 'Creates the in-memory authorization service used to retain issued access-token metadata during the process lifetime.'
        'SecurityConfiguration.authorizationServerSettings' = 'Configures the canonical issuer identifier advertised and enforced by Spring Authorization Server.'
        'SecurityConfiguration.authorizationServerChain' = 'Configures OAuth authorization endpoints, OIDC defaults, custom grant converters/providers, CSRF exclusions for protocol endpoints, authenticated-client enforcement, and the browser login entry point.'
        'SecurityConfiguration.applicationChain' = 'Defines the fallback Authorization Server web policy, including public health/API documentation and the form-login defaults.'
        'AccessTokenIssuer.issue' = 'Creates an OAuth token context containing the client, subject principal, authorized scopes, audience, and grant; generates the signed JWT; persists its authorization metadata; and returns the protocol access-token response.'
        'OAuth2GrantRequestParameters.from' = 'Extracts the first non-empty submitted value for each form parameter other than grant_type and returns those values as the custom grant parameter map.'
        'OAuth2GrantRequestSupport.requireAuthenticatedClient' = 'Requires the grant principal to be an authenticated OAuth client and raises invalid_client when the precondition is not met.'
        'OAuth2GrantRequestSupport.requestedScopes' = 'Uses the complete allowed scope set when scope is omitted; otherwise parses whitespace-separated requested values and rejects empty or unauthorized scopes with invalid_scope.'
        'OAuth2GrantRequestSupport.requestedAudience' = 'Selects the configured default audience when none is supplied and rejects any selected URI outside the explicit audience allow-list.'
        'SubjectGrantAuthenticationConverter.convert' = 'Returns null for unrelated OAuth grants; for the local subject grant, builds an authentication request from the current client principal and non-grant form parameters.'
        'SubjectGrantAuthenticationProvider.authenticate' = 'Validates the authenticated client, registered grant permission, bounded non-empty caller-supplied subject, and allowed scopes, then delegates token signing. The subject value is not authenticated as a user identity.'
        'SubjectGrantAuthenticationProvider.supports' = 'Reports whether Spring Security can route the supplied authentication class to this subject-grant provider.'
        'SubjectGrantAuthenticationToken.SubjectGrantAuthenticationToken' = 'Creates a subject-grant authentication request carrying the authenticated client principal and submitted grant parameters.'
        'TokenExchangeAuthenticationConverter.convert' = 'Returns null for unrelated OAuth grants; for RFC 8693, builds an exchange authentication request from the current client principal and submitted source-token parameters.'
        'TokenExchangeAuthenticationProvider.authenticate' = 'Validates the registered client and RFC 8693 token-type parameters, decodes the trusted source JWT, intersects its scopes with client permissions, validates the requested audience, and delegates issuance of the exchanged access token.'
        'TokenExchangeAuthenticationProvider.supports' = 'Reports whether Spring Security can route the supplied authentication class to the RFC 8693 exchange provider.'
        'TokenExchangeAuthenticationToken.TokenExchangeAuthenticationToken' = 'Creates an RFC 8693 grant authentication request carrying the authenticated OAuth client and submitted exchange parameters.'
        'ResourceServerApplication.main' = 'Starts the protected Resource Server Spring Boot application and delegates process configuration and shutdown handling to SpringApplication.'
        'PortageCyberTechController.hello' = 'Returns the JSON greeting for the authenticated bearer-token subject. Access control and api.read scope enforcement occur in the Resource Server security filter chain before this controller runs.'
        'ResourceSecurityConfiguration.jwtDecoder' = 'Creates a remote-JWKS JWT decoder with Spring''s standard issuer/time checks and an additional required API-audience validator.'
        'ResourceSecurityConfiguration.resourceServerChain' = 'Protects every /api/** request with JWT bearer authentication and requires the SCOPE_api.read authority.'
        'ResourceSecurityConfiguration.applicationChain' = 'Configures the fallback web chain, allowing health/API documentation routes and keeping non-API application routes outside the protected resource matcher.'
        'JwtTokenDecoder.JwtTokenDecoder' = 'Injects the Spring Security JWT decoder, JSON mapper, HTTP client builder, and configured JWKS URI used by token-inspection operations.'
        'JwtTokenDecoder.decode' = 'Validates the supplied compact JWT through Spring Security before extracting its Nimbus header, claims, and encoded signature; malformed or untrusted tokens fail with a JWT exception.'
        'JwtTokenDecoder.getJwtHeader' = 'Returns the JOSE header of the JWT only after full configured decoder validation.'
        'JwtTokenDecoder.getClaimsJson' = 'Serializes the validated JWT claims as JSON and reports a serialization failure explicitly.'
        'JwtTokenDecoder.toBearerAuthorizationValue' = 'Normalizes a non-empty token as an HTTP Authorization header value while avoiding a duplicate Bearer prefix.'
        'JwtTokenDecoder.getExpiresAt' = 'Returns the validated JWT expiration instant and rejects tokens without an expiration or whose expiration is not in the future.'
        'JwtTokenDecoder.getSigningAlgorithm' = 'Reads the compact JWT header and returns its signing algorithm, rejecting malformed tokens and non-signature JOSE algorithms.'
        'JwtTokenDecoder.getSigningPublicKey' = 'Reads the token kid, fetches the configured JWKS document, and returns the matching RSA public key; missing identifiers, empty JWKS, or non-RSA keys fail explicitly.'
        'JwtTokenDecoder.hasValidSignature' = 'Verifies that the compact JWT uses RS256 and checks its signature with the RSA public key selected from the authorization server JWKS.'
        'JwtTokenDecoder.getAudiences' = 'Returns the audience claims from a JWT accepted by the configured Spring Security decoder.'
        'JwtTokenValidation.JwtTokenValidation' = 'Injects the token-decoding collaborator used to validate and describe access tokens.'
        'JwtTokenValidation.validate' = 'Removes an optional Bearer prefix, validates the signature and expiration, collects header/claims/algorithm/key/audience metadata, and returns an immutable validation report; invalid input is reported as an exception.'
        'JwtTokenValidation.isValid/1' = 'Returns true only when the configured validation workflow succeeds; expected JWT, input, key-service, and HTTP-client failures produce false.'
        'JwtTokenValidation.isValid/2' = 'Builds a Spring Security decoder for the supplied JWKS URI and returns whether it accepts the normalized token, returning false for invalid inputs or expected decoder/network failures.'
        'ProtectReportPdfs.main' = 'Validates command-line arguments, reads the PDF opening password from standard input, discovers every PDF beneath the requested root, applies AES-256 encryption and denied modification/printing/extraction permissions, verifies the temporary result, then safely replaces each original while preserving its modification timestamp.'
        'ProtectReportPdfs.isAlreadyProtected' = 'Determines whether the file is already encrypted with the supplied opening password and verifies all required restrictions; it refuses to overwrite a file protected by an unrecognized password.'
        'ProtectReportPdfs.verifyPermissions' = 'Checks that PDF encryption is active and that printing, content extraction, accessibility extraction, modification, annotations, form filling, and assembly are all disallowed.'
        'ProtectReportPdfs.randomToken' = 'Generates a cryptographically random hexadecimal owner password for PDF permission administration, independent of the user opening password.'
        'PdfProtectionTestSupport.createPdf' = 'Creates a minimal one-page PDF fixture in the supplied temporary path for repeatable protection tests.'
        'PdfProtectionTestSupport.createPdf/1' = 'Creates parent directories and saves a valid one-page PDF fixture at the requested temporary destination.'
        'PdfProtectionTestSupport.protectReports' = 'Resolves the packaged PowerShell resource, prepares an isolated process environment, supplies a test-only password without putting it on the command line, and fails with captured process output if protection does not complete successfully.'
        'PdfProtectionTestSupport.assertProtected' = 'Verifies that an empty-password open is rejected and that the supplied password opens the PDF with every prohibited permission disabled.'
        'PdfProtectionTestSupport.isWindows' = 'Selects the Windows PowerShell executable when the current JVM runs on Windows.'
        'PdfProtectionTestSupport.isExecutableAvailable' = 'Checks whether the selected PowerShell host starts successfully within a bounded wait, restoring the interrupt flag when the probe is interrupted.'
        'ProtectReportPdfsUnitTest.protectsASinglePdfAndRestrictsItsPermissions' = 'Creates one temporary PDF, runs the packaged protection script, and asserts that password and restrictive permissions were applied.'
        'ProtectReportPdfsIntegrationTest.protectsPdfFilesInNestedReportDirectories' = 'Creates French and English report fixtures in nested paths, runs recursive protection once, and verifies both resulting PDFs.'
        'ProtectReportPdfsSystemTest.protectsAnEntireReportTreeWithoutChangingNonPdfFiles' = 'Exercises a multi-level report tree and verifies both protected PDFs and byte-preserving treatment of a non-PDF companion.'
        'ProtectReportPdfsMicroserviceTest.protectsAuthorizationAndResourceServiceReports' = 'Exercises separate Authorization Server and Resource Server report folders in one protection run and verifies both outputs.'
    }
    $overloadKey = "$ClassName.$Name/$ParameterCount"
    if ($methodDescriptions.ContainsKey($overloadKey)) {
        return $methodDescriptions[$overloadKey]
    }
    $key = "$ClassName.$Name"
    if ($methodDescriptions.ContainsKey($key)) {
        return $methodDescriptions[$key]
    }
    if ($Name -eq $ClassName) {
        return "Initializes the $ClassName instance with the supplied collaborators and configuration."
    }
    if ($Name -match '^(test|returns|rejects|accepts|exposes|publishes|issues|retrieves|creates|builds|requires|configures|adds|formats|verifies|defaults|isValid|helloWorld|check|request|start|stop|register|decode|validate|convert|authenticate|supports|get|to|has|signed|tamper|encode|serviceUrl|read|issue|obtain)') {
        $words = [regex]::Replace($Name, '([a-z0-9])([A-Z])', '$1 $2').ToLowerInvariant()
        if ($ReturnType -eq 'void') {
            return "Performs the '$words' operation and enforces its documented contract."
        }
        return "Performs the '$words' operation and returns the corresponding result."
    }
    $words = [regex]::Replace($Name, '([a-z0-9])([A-Z])', '$1 $2').ToLowerInvariant()
    return "Performs the '$words' operation required by this class."
}

function New-MethodDocumentation {
    param(
        [string] $Indent,
        [string] $Name,
        [string] $ClassName,
        [string] $ReturnType,
        [string] $ParameterText,
        [string] $ThrowsText
    )
    $lines = New-Object 'System.Collections.Generic.List[string]'
    $lines.Add("$Indent/**")
    $lines.Add("$Indent * <!-- Generated by Document-Java-Api.ps1 -->")
    $parameters = Split-ParameterList $ParameterText
    $description = Get-MethodDescription $Name $ClassName $ReturnType $parameters.Count
    foreach ($line in ($description -split "`n")) {
        $lines.Add("$Indent * $line")
    }
    foreach ($parameter in $parameters) {
        $parameterName = Get-ParameterName $parameter
        $lines.Add("$Indent * @param $parameterName $(Get-ParameterDescription $parameterName)")
    }
    if ($ReturnType -and $ReturnType -ne 'void' -and $Name -ne $ClassName) {
        $lines.Add("$Indent * @return the result described above.")
    }
    if ($ThrowsText) {
        $exceptionNames = $ThrowsText -replace '^throws\s+', ''
        foreach ($exceptionName in ($exceptionNames -split '\s*,\s*')) {
            if ($exceptionName -match '^[A-Za-z_$][\w.$]*$') {
                $lines.Add("$Indent * @throws $exceptionName when the operation cannot complete its contract")
            }
        }
    }
    $lines.Add("$Indent */")
    return ($lines -join "`r`n") + "`r`n"
}

function New-TypeDocumentation {
    param(
        [string] $Indent,
        [string] $TypeName,
        [string] $TypeKind,
        [string] $SourcePath,
        [string] $RecordParameters
    )
    $description = $descriptions[$TypeName]
    if (-not $description) {
        $description = "Nested $TypeKind '$TypeName' used by $([System.IO.Path]::GetFileNameWithoutExtension($SourcePath)); it supports the containing class contract and is not a separate application service."
    }
    $lines = New-Object 'System.Collections.Generic.List[string]'
    $lines.Add("$Indent/**")
    $lines.Add("$Indent * <!-- Generated by Document-Java-Api.ps1 -->")
    $lines.Add("$Indent * $description")
    $lines.Add("$Indent *")
    $lines.Add("$Indent * <p>Project demonstration metadata: Portage CyberTech, release $ReleaseVersion, documentation date $DocumentationDate.</p>")
    if ($TypeKind -eq 'record') {
        foreach ($parameter in (Split-ParameterList $RecordParameters)) {
            $parameterName = Get-ParameterName $parameter
            $lines.Add("$Indent * @param $parameterName $(Get-ParameterDescription $parameterName)")
        }
    }
    $lines.Add("$Indent * @author $author")
    $lines.Add("$Indent * @version $ReleaseVersion")
    $lines.Add("$Indent * @since $ReleaseVersion")
    $lines.Add("$Indent */")
    return ($lines -join "`r`n") + "`r`n"
}

$typePattern = [regex]::new(
    '(?m)^(?<indent>[ \t]*)(?:(?:public|protected|private|abstract|static|final|sealed|non-sealed)\s+)*(?<kind>class|record|interface|enum)\s+(?<name>[A-Za-z_$][\w$]*)',
    [System.Text.RegularExpressions.RegexOptions]::Multiline)
$methodPattern = [regex]::new(
    '(?m)^(?<indent>[ \t]*)(?<prefix>(?:(?:public|protected|private|static|final|synchronized|abstract|default|native|strictfp)[ \t]+)*)(?:(?<return>[\w.$<>\[\], ?]+?)[ \t]+)?(?<name>[A-Za-z_$][\w$]*)[ \t]*\((?<parameters>[^{};]*?)\)[ \t\r\n]*(?<throws>throws\s+[\w.$,\s]+?)?[ \t\r\n]*\{',
    [System.Text.RegularExpressions.RegexOptions]::Multiline)
$controlWords = @('if', 'for', 'while', 'switch', 'catch', 'try', 'synchronized', 'new', 'return', 'throw', 'else', 'do')

function Remove-GeneratedDocumentation {
    param([string] $Source)
    return [regex]::Replace(
        $Source,
        '(?ms)^[ \t]*/\*\*.*?^[ \t]*\*/\r?\n',
        {
            param($match)
            if ($match.Value.Contains('<!-- Generated by Document-Java-Api.ps1 -->')) {
                return ''
            }
            return $match.Value
        }
    )
}

$javaFiles = @(Get-ChildItem -LiteralPath $ProjectRoot -Recurse -File -Filter '*.java' |
    Where-Object { $_.FullName -notmatch '[\\/]target[\\/]' })

if ($javaFiles.Count -eq 0) {
    throw "No Java source files were found under $ProjectRoot."
}

$documentedFiles = 0
$documentedTypes = 0
$documentedMethods = 0
foreach ($file in $javaFiles) {
    $source = [System.IO.File]::ReadAllText($file.FullName)
    $source = Remove-GeneratedDocumentation $source

    $edits = New-Object 'System.Collections.Generic.List[object]'
    foreach ($typeMatch in $typePattern.Matches($source)) {
        $typeName = $typeMatch.Groups['name'].Value
        $kind = $typeMatch.Groups['kind'].Value
        $recordParameters = ''
        if ($kind -eq 'record') {
            $recordMatch = [regex]::Match($source.Substring($typeMatch.Index), '\brecord\s+' + [regex]::Escape($typeName) + '\s*\((?<parameters>[^)]*)\)')
            if ($recordMatch.Success) {
                $recordParameters = $recordMatch.Groups['parameters'].Value
            }
        }
        $indent = $typeMatch.Groups['indent'].Value
        $edits.Add(@{
            Index = $typeMatch.Index
            Text = New-TypeDocumentation $indent $typeName $kind $file.FullName $recordParameters
        })
        $documentedTypes++
    }

    $typeMatches = @($typePattern.Matches($source))
    $outerTypeName = if ($typeMatches.Count -gt 0) { $typeMatches[0].Groups['name'].Value } else { [System.IO.Path]::GetFileNameWithoutExtension($file.Name) }
    foreach ($methodMatch in $methodPattern.Matches($source)) {
        $methodName = $methodMatch.Groups['name'].Value
        if ($controlWords -contains $methodName) {
            continue
        }
        $returnType = $methodMatch.Groups['return'].Value.Trim()
        $prefix = $methodMatch.Groups['prefix'].Value
        if (-not $returnType -and $prefix.Trim().Length -eq 0) {
            continue
        }
        if ($returnType -match '^(class|record|interface|enum)$') {
            continue
        }
        $containingType = $outerTypeName
        foreach ($typeMatch in $typeMatches) {
            if ($typeMatch.Index -lt $methodMatch.Index) {
                $containingType = $typeMatch.Groups['name'].Value
            } else {
                break
            }
        }
        $indent = $methodMatch.Groups['indent'].Value
        $documentation = New-MethodDocumentation `
            $indent $methodName $containingType $returnType `
            $methodMatch.Groups['parameters'].Value $methodMatch.Groups['throws'].Value
        $edits.Add(@{
            Index = $methodMatch.Index
            Text = $documentation
        })
        $documentedMethods++
    }

    $orderedEdits = @($edits.ToArray() | Sort-Object -Property @{ Expression = { [int]$_.Index }; Descending = $true })
    foreach ($edit in $orderedEdits) {
        $source = $source.Insert($edit.Index, $edit.Text)
    }
    [System.IO.File]::WriteAllText($file.FullName, $source, [System.Text.UTF8Encoding]::new($false))
    $documentedFiles++
}

Write-Output "Documented $documentedFiles Java files, $documentedTypes types, and $documentedMethods methods for release $ReleaseVersion ($DocumentationDate)."
