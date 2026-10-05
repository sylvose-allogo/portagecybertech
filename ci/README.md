# CI configuration

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** CI configuration.*Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-04  
> **Purpose:** CI configuration.


This repository is one Maven reactor with three modules, not three independent
Maven roots:

| Module | CI treatment |
|---|---|
| `authorization-server` | Unit/contract tests and executable service package |
| `resource-server` | Unit/contract tests and executable service package |
| `integration-tests` | Test-scoped cross-service and Cucumber contracts; never deployed |

Both CI definitions run the same reactor verification (`mvn clean verify`) and
archive the two executable Spring Boot JARs. The integration-test module is
included in verification. Neither pipeline deploys to a server or publishes
images; those operations require a separately approved registry/target and
credentials.

- [`jenkins/`](jenkins/README.md): isolated Jenkins controller, JCasC, plugins,
  and a repository `Jenkinsfile`.
- [`concourse/`](concourse/README.md): isolated local Concourse deployment,
  declarative YAML pipeline, Git resource, and YAML task.

Both CI systems require a Linux build worker with JDK 23 and Maven 3.9 or newer.
Keep their controllers and workers separate from production services. Do not
put credentials into pipeline YAML or source-controlled environment files.


## Bilingual Postman endpoint report

The consolidated report brings together the Dev environment and historical results. French and English editions are available in HTML, Word, and PDF: [français](../documents/rapports/04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../documents/rapports/04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
