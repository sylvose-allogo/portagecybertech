# Project Management & Deliverables

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-04
> **Purpose:** Project management and deliverable tracking — software artifact register, deliverable catalogue, technology selection, and traceability matrices.

This directory contains the project management and deliverable tracking artifacts for the Mini OAuth 2.0 platform. It covers the full software artifact register, the deliverable catalogue with gap analysis, technology selection decisions, and the requirements traceability matrix. Narrative reports are available in both French and English; the CSV source files are language-neutral.

## Reports

| French (`../../../../TestPortageCyberTech/documents/rapports/05_Gestion-et-Livrables/fr`) | English (`../../../../TestPortageCyberTech/documents/rapports/05_Gestion-et-Livrables/en`) | Formats |
|---|---|---|
| `referentiel-artefacts-logiciels` | `software-artifact-register` | HTML, DOCX, PDF |
| `catalogue-livrables-ecarts` | `deliverable-register-and-decisions` | HTML, DOCX, PDF |
| `choix-technologiques-plateforme` | `technology-selection-for-oauth2-platform` | HTML, DOCX, PDF |

### `../../../../TestPortageCyberTech/documents/rapports/05_Gestion-et-Livrables/fr` — French editions

- `referentiel-artefacts-logiciels.html` / `.docx` / `.pdf` — Comprehensive register of all software artifacts produced by the project: source modules, configuration files, scripts, reports, and model sources, with their location and status.
- `catalogue-livrables-ecarts.html` / `.docx` / `.pdf` — Deliverable catalogue listing all planned deliverables, their actual delivery status, and any identified gaps or deviations.
- `choix-technologiques-plateforme.html` / `.docx` / `.pdf` — Technology selection document justifying the choice of Spring Boot, Spring Authorization Server, JWT/JWK, Vault, and supporting libraries for the platform.

### `../../../../TestPortageCyberTech/documents/rapports/05_Gestion-et-Livrables/en` — English editions

- `software-artifact-register.html` / `.docx` / `.pdf` — English edition of the software artifact register.
- `deliverable-register-and-decisions.html` / `.docx` / `.pdf` — English edition of the deliverable catalogue and gap analysis.
- `technology-selection-for-oauth2-platform.html` / `.docx` / `.pdf` — English edition of the technology selection document.

## CSV source files

The following CSV files are the machine-readable sources for the project management data. They are language-neutral and can be opened in any spreadsheet application.

- `../../../../TestPortageCyberTech/documents/rapports/05_Gestion-et-Livrables/Matrice-de-tracabilite-exigences.csv` — Requirements traceability matrix mapping each requirement from the functional specification to its implementing component, test case, and validation status.
- `../../../../TestPortageCyberTech/documents/rapports/05_Gestion-et-Livrables/Referentiel-des-artefacts-logiciels.csv` — Tabular source data for the software artifact register: artifact name, type, path, responsible party, and delivery status.
- `../../../../TestPortageCyberTech/documents/rapports/05_Gestion-et-Livrables/Registre-des-risques.csv` — Risk register listing identified project risks, their likelihood, impact, mitigation actions, and current status.

## Relationship to other deliverables

- The requirements traced in `../../../../TestPortageCyberTech/documents/rapports/05_Gestion-et-Livrables/Matrice-de-tracabilite-exigences.csv` originate from `../01_Cadrage/`.
- The artifacts catalogued in `../../../../TestPortageCyberTech/documents/rapports/05_Gestion-et-Livrables/Referentiel-des-artefacts-logiciels.csv` span all six report directories plus the source code modules.
- Test results that close traceability items are in `../../../../TestPortageCyberTech/documents/rapports/04_Tests-et-Validation`.

## Regenerating exports

Word and PDF exports can be regenerated from the HTML sources using:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\documents\rapports\06_Outils-de-Generation\Generer-Rapports.ps1
```

This script requires Microsoft Word to be installed on Windows.

## Consolidated Postman report

The consolidated endpoint test report brings together the Dev environment and historical results. French and English editions are available in HTML, Word and PDF: [French](../../../../TestPortageCyberTech/documents/rapports/04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../../../../TestPortageCyberTech/documents/rapports/04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
