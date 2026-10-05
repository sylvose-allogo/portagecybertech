# Maven and Java API documentation helpers

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** Maven and Java API documentation helpers.@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-04  
> **Purpose:** Maven and Java API documentation helpers.


Run the Maven launcher from the project root or any other working directory:

```cmd
.mvn\maven.cmd
.mvn\maven.cmd clean test
.mvn\maven.bat clean verify
```

With no arguments, either launcher runs the reactor's `verify` lifecycle. With
arguments, they pass the requested goals and options to Maven while explicitly
selecting the root `pom.xml`. Maven can be resolved from `MAVEN_HOME`, `M2_HOME`,
`PATH`, or the local `C:\apache-maven-3.10.0` installation.

Generate or refresh the JavaDoc comments in all production, test, and nested
Java types with:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\.mvn\Document-Java-Api.ps1
```

The generator documents class purpose, demonstration metadata, method
parameters, return values, and declared exceptions. Its default release
metadata is version `1.0.0` and date `2026-10-04`; override these with
`-ReleaseVersion` and `-DocumentationDate` when maintaining a later release.


## Bilingual Postman endpoint report

The consolidated report brings together the Dev environment and historical results. French and English editions are available in HTML, Word, and PDF: [français](../documents/rapports/04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../documents/rapports/04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
