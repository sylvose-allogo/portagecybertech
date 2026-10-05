# Document generation tools — English

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-04  
> **Purpose:** Document generation tools — English.


This language folder documents the tooling without moving scripts or the
source logo. `Generer-Documents-Conception.ps1` creates Word/PDF copies of the
French and English design reports. `Convertir-HTML-en-Word-PDF.ps1` exports
the HTML requirements, architecture and management documents.

The Java utility and `Protect-Report-Pdfs.ps1` now reside under
`integration-tests/src/test/`, with tests that operate on temporary reports.
PDF restrictions depend on compatible readers and cannot prevent screenshots,
photography, or software that ignores them. The official logo remains in the
`06_Outils-de-Generation` folder.


## Bilingual Postman endpoint report

The consolidated report brings together the Dev environment and historical results. French and English editions are available in HTML, Word, and PDF: [français](../../04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../../04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
