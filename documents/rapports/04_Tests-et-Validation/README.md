# Testing & Validation

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05
> **Purpose:** Testing and validation deliverables — unit tests, integration tests, OAuth 2.0 contract tests, and Postman endpoint test reports.

This directory contains all testing and validation deliverables for the Mini OAuth 2.0 platform. It covers unit test results, integration test results, OAuth 2.0 contract test reports, consolidated Postman endpoint test reports, and the full set of Postman automation sources (collection, environments, scripts, and screenshot evidence). All reports are delivered in both French and English.

## Reports

| French (`fr/`) | English (`en/`) | Formats |
|---|---|---|
| `rapport-tests-unitaires` | `unit-test-results` | HTML, DOCX, PDF |
| `rapport-tests-integration` | `integration-test-results` | HTML, DOCX, PDF |
| `rapport-tests-postman` | *(see consolidated report below)* | DOCX |
| `Rapport-des-tests-Endpoint-Postman-Env-Dev` | `Postman-Endpoint-Test-Report-Dev-Environment` | HTML, DOCX, PDF |
| `Historique-des-demandes-et-reponses-Mini-plateforme-OAuth2` | `Structured-history-of-requests-and-responses-for-the-OAuth2-mini-platform` | HTML, DOCX, PDF |
| *(French only)* | `oauth2-contract-test-report` | HTML, DOCX, PDF |

### `fr/` — French editions

- `rapport-tests-unitaires.html` / `.docx` / `.pdf` — Unit test results for the Authorization Server and Resource Server Spring Boot modules.
- `rapport-tests-integration.html` / `.docx` / `.pdf` — Integration test results covering end-to-end OAuth 2.0 flows between the two services.
- `rapport-tests-postman.html` / `.docx` / `.pdf` — Postman test report (earlier edition, superseded by the consolidated report below).
- `Rapport-des-tests-Endpoint-Postman-Env-Dev.html` / `.docx` / `.pdf` — Consolidated Postman endpoint test report for the Dev environment, including historical results.
- `Historique-des-demandes-et-reponses-Mini-plateforme-OAuth2.html` / `.docx` — Structured history of project questions, decisions, and responses.

### `en/` — English editions

- `unit-test-results.html` / `.docx` / `.pdf` — English edition of the unit test results.
- `integration-test-results.html` / `.docx` / `.pdf` — English edition of the integration test results.
- `Postman-Endpoint-Test-Report-Dev-Environment.html` / `.docx` / `.pdf` — English edition of the consolidated Postman endpoint test report.
- `Structured-history-of-requests-and-responses-for-the-OAuth2-mini-platform.html` / `.docx` / `.pdf` — English edition of the structured history of project requirements and decisions. French edition is `Historique-des-demandes-et-reponses-Mini-plateforme-OAuth2` in `fr/`.
- `oauth2-contract-test-report.html` / `.docx` / `.pdf` — OAuth 2.0 contract test report verifying compliance of the Authorization Server and Resource Server with the RFC 6749 / RFC 8693 contracts.

## Postman sources and evidence

The `Postman/` subdirectory contains the full automation suite used to produce the endpoint test reports.

### `Postman/Scripts/` — Automation scripts

- `Portage CyberTech - OAuth2 End-to-End.postman_collection.json` — Postman collection covering all endpoints: health checks, JWKS public key retrieval, Subject Grant token issuance, RFC 8693 Token Exchange, protected Resource Server endpoint, and rejection scenarios.
- `environments/Env Dev.postman_environment.json` — Dev environment variables.
- `environments/Env Int.postman_environment.json` — Integration environment variables.
- `environments/Env Prod.postman_environment.json` — Production environment variables.
- `run-collection.ps1` — PowerShell script to execute the Postman collection with Newman against the target environment.
- `start-authorization-server.ps1` — PowerShell script to start the Authorization Server microservice locally.
- `start-resource-server.ps1` — PowerShell script to start the Resource Server microservice locally.

### `Postman/Preuves/` — Screenshot evidence

Raw Postman screenshots captured during test execution, one per tested scenario:

| # | Scenario |
|---|---|
| 01 | Authorization Server — health check |
| 02 | Resource Server — health check |
| 03 | Authorization Server — JWKS public key endpoint |
| 04 | Authorization Server — local demo source token (localhost simulation only) |
| 05 | Authorization Server — RFC 8693 token exchange |
| 06 | Resource Server — protected `/hello` with OAuth 2.0 Bearer |
| 07 | Resource Server — rejects missing Bearer |
| 08 | Resource Server — rejects modified JWT signature |
| 09 | Authorization Server — rejects invalid client |
| 10 | Authorization Server — rejects invalid RFC 8693 source token |

Additional composite screenshots: microservice startup, endpoint demo overview, health check summary.

> **Security note:** Raw screenshots and the archived historical report may contain encoded JWT or Basic authentication headers. The consolidated Word report includes redacted copies of authentication values. The originals are sensitive evidence and should be kept in a controlled-access location and not distributed publicly.

### `Postman/Rapports-historiques/` — Historical reports

Archived test reports from earlier project phases. Historical results retain their original dates and do not establish current execution status.

- `Rapport-des-tests-endpoints-Postman.docx` — Archived historical Postman test report.
- `integration-tests.html` — Archived integration test results (HTML).
- `unit-tests.html` — Archived unit test results (HTML).
- `postman-Env-Int.xml` / `postman-Env-Test.xml` / `postman-Env-Prod.xml` — Legacy Postman environment exports (XML format).

## Relationship to other deliverables

- Requirements that these tests validate are defined in `../01_Cadrage/`.
- The component design under test is documented in `../03_Conception/`.
- Traceability between requirements and test coverage is in `../05_Gestion-et-Livrables/Matrice-de-tracabilite-exigences.csv`.

## Regenerating exports

Word and PDF exports can be regenerated from the HTML sources using:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\documents\rapports\06_Outils-de-Generation\Generer-Rapports.ps1
```

HTML-only sources can also be converted individually:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\documents\rapports\06_Outils-de-Generation\Convertir-HTML-en-Word-PDF.ps1
```

Both scripts require Microsoft Word to be installed on Windows.

## Consolidated Postman report

The consolidated endpoint test report brings together the Dev environment and historical results. French and English editions are available in HTML, Word and PDF: [French](fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](en/Postman-Endpoint-Test-Report-Dev-Environment.html).
