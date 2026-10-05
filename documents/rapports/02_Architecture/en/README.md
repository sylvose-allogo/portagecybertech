# OAuth 2.0 architecture — English


> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** OAuth 2.0 architecture — English deliverables.


This directory contains the English edition of all architecture deliverables
for the Portage CyberTech mini OAuth 2.0 platform. Each document is available
in HTML, Word (DOCX) and PDF formats. French equivalents are in `../fr/`.

## Documents

| File (base name) | Description |
|---|---|
| `oauth2-platform-architecture` | Main architecture dossier: context, application, component, physical deployment, OAuth 2.0 sequence, decision log and requirements traceability views. |
| `recommended-microservices-architecture` | Recommended microservices architecture: design patterns, security boundaries, secret-storage options and production limitations of the current implementation. |
| `oauth2-bpmn-process-description` | Detailed BPMN process description: token acquisition, token exchange (RFC 8693) and protected resource access flow. |
| `technical-models-and-formats-guide` | Guide to the modelling notations used: BPMN, UML, EAM (Enterprise Application Mapping), DMN, BPEL/WSDL and related formats. |
| `bpel-process-reference-note` | Conceptual BPEL orchestration note for token exchange and a protected API call; requires a BPEL engine and REST adapters to execute. |
| `Practical-Microservices-Architectural-Patterns` | Reference book: *Practical Microservices Architectural Patterns — Event-Based Java Microservices with Spring Boot and Spring Cloud*. |

## Architecture views covered

The main dossier (`oauth2-platform-architecture`) includes the following views,
sourced from the PowerDesigner diagrams under `../Power_Designer/Diagrammes/`:

| View | Source diagram |
|---|---|
| Context diagram | `01-Architecture-contexte.png` |
| Application architecture | `02-Architecture-applicative.png` |
| Physical deployment | `03-Architecture-physique.png` |
| Component diagram | `04-Diagramme-composants.png` |
| Use-case diagram | `05-Cas-utilisation.png` |
| BPMN token process | `06-Processus-BPMN.png` |
| OAuth 2.0 sequence | `07-Sequence-OAuth2.png` |
| BPEL orchestration reference | `08-BPEL-orchestration-reference.png` |

## Related deliverables

- **Functional and technical requirements:** `../../01_Cadrage/en/functional-and-technical-requirements` (HTML, DOCX, PDF)
- **Detailed software design:** `../../03_Conception/en/detailed-design-mini-oauth2-platform` (HTML, DOCX, PDF)
- **Source models and diagrams:** `../Power_Designer/` (BPMN, UML, EAM, DMN, BPEL)
- **Test reports and Postman collection:** `../../04_Tests-et-Validation/en/`
- **French edition of these reports:** `../fr/`

## Regenerating documents

- `../Scripts/Generate-PowerDesigner-Models.ps1` — regenerates PowerDesigner
  native models and diagram images. Does **not** produce the reports in this
  directory.
- `../Scripts/Generate-Architecture-Documents.ps1` — generates document figures
  and DOCX/PDF outputs from the configured HTML sources.
- `../../06_Outils-de-Generation/Convertir-HTML-en-Word-PDF.ps1` — converts HTML
  reports to Word and PDF using Microsoft Word.

## Scope and caveats

The architecture reports describe observed interfaces and proposed design.
They do not claim that a production cloud topology, API gateway, durable
database, high-availability cluster or external identity provider has been
deployed. The running Spring Boot services expose REST/JSON only. The BPEL
process and WSDL definitions are abstract reference artifacts and require a
BPEL engine and REST adapters before they could be executed. Demo credentials,
caller-supplied subject grants, and in-memory key/client configuration are not
production defaults. Historical test results retain their original dates and do
not establish current status.


## Bilingual Postman endpoint report

The consolidated report brings together the Dev environment and historical results. French and English editions are available in HTML, Word, and PDF: [français](../../04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../../04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
