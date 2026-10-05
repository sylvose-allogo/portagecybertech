# Authorization and Resource Server Contract Tests

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** Authorization and Resource Server Contract Tests.om · **Version:** 1.0.0 · **Date:** 2026-10-04  
> **Purpose:** Authorization and Resource Server Contract Tests.


This Maven module contains only test-scoped dependencies on the Authorization
Server and Resource Server. It is not an independently deployable service.
Its JUnit and Cucumber tests verify the real HTTP, OAuth token and JWKS
contracts across the two services.

The `com.portagecybertech.ca.integration.util` package contains the PDF
protection utility and its supporting unit, integration, system, and
microservice-level tests. Class and method contracts, parameters, results,
and declared exceptions are documented in JavaDoc with project attribution.
The detailed PowerShell runner is in
`src/test/resources/scripts/Protect-Report-Pdfs.ps1`; tests pass temporary
report directories so they never alter the checked-in report archive. These
tests require PowerShell and use test-scoped PDFBox.

From the repository root, run `mvn -pl integration-tests -am test` to build the
two services and execute the test module, or run `mvn clean verify` to test
the complete reactor. The Jenkins and Concourse pipelines run the latter so
unit, service-contract and cross-service tests are included before packaging.


## Bilingual Postman endpoint report

The consolidated report brings together the Dev environment and historical results. French and English editions are available in HTML, Word, and PDF: [français](../documents/rapports/04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../documents/rapports/04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
