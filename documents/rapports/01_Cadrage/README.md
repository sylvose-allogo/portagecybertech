# Scoping & Requirements

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05
> **Purpose:** Scoping and requirements deliverables — project charter, feasibility study, and functional & technical specification.

This directory contains the initial project scoping deliverables for the Mini
OAuth 2.0 platform. It covers the project scope and feasibility analysis, as
well as the functional and technical requirements specification. All documents
are available in both French and English, each in HTML, Word, and PDF formats.

## Documents

| French (`fr/`) | English (`en/`) | Formats |
|---|---|---|
| `etude-de-faisabilite-portage-cybertech` | `project-scope-and-feasibility` | HTML, DOCX, PDF |
| `cahier-des-charges-fonctionnel-technique` | `functional-and-technical-requirements` | HTML, DOCX, PDF |

### `fr/` — French editions

- `etude-de-faisabilite-portage-cybertech.html` / `.docx` / `.pdf` — Feasibility
  study: context, objectives, constraints, risks, and project viability for the
  Mini OAuth 2.0 platform at Portage CyberTech.
- `cahier-des-charges-fonctionnel-technique.html` / `.docx` / `.pdf` — Functional
  and technical requirements specification: use cases, security requirements,
  OAuth 2.0 grant flows (Subject Grant, Token Exchange RFC 8693), JWT signing,
  and integration constraints.

### `en/` — English editions

- `project-scope-and-feasibility.html` / `.docx` / `.pdf` — English edition of
  the feasibility study.
- `functional-and-technical-requirements.html` / `.docx` / `.pdf` — English
  edition of the functional and technical requirements specification.

## Relationship to other deliverables

- The architecture deliverables that implement these requirements are in
  `../02_Architecture/`.
- The detailed design that translates these requirements into technical
  components is in `../03_Conception/`.
- Traceability between these requirements and implemented features is tracked in
  `../05_Gestion-et-Livrables/Matrice-de-tracabilite-exigences.csv`.

## Regenerating exports

Word and PDF exports can be regenerated from the HTML sources using:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\documents\rapports\06_Outils-de-Generation\Convertir-HTML-en-Word-PDF.ps1
```

This script requires Microsoft Word to be installed and processes each HTML
file in place, producing `.docx` and `.pdf` files alongside the source.


## Consolidated Postman report

The consolidated endpoint test report brings together the Dev environment and historical results. French and English editions are available in HTML, Word and PDF: [French](../04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
