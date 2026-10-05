# OAuth 2.0 architecture deliverables


> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** OAuth 2.0 architecture deliverables index and model sources.


This directory contains all architecture deliverables for the Portage
CyberTech mini OAuth 2.0 platform. Human-readable reports are separated by
language under `en/` and `fr/`; editable source models and generated diagrams
are under `Power_Designer/`.

## Directory structure

```
02_Architecture/
├── en/                          # English reports (HTML, DOCX, PDF)
├── fr/                          # French reports (HTML, DOCX, PDF)
├── Power_Designer/
│   ├── BPMN/                    # PortageCyberTech-OAuth2-Process.bpb
│   ├── UML/                     # PortageCyberTech-Use-Cases-and-Components.oom
│   ├── EAM/                     # PortageCyberTech-Application-and-Physical-Architecture.eam
│   ├── DMN/                     # PortageCyberTech-DMN-Decision-Access-Control.dmn
│   ├── BPEL/                    # Abstract BPEL contracts and WSDL definitions
│   ├── Modeles/                 # Additional model sources
│   ├── Diagrammes/              # Generated diagram images (PNG)
│   └── Workspace.sws            # PowerDesigner workspace file
├── Scripts/
│   ├── Generate-PowerDesigner-Models.ps1
│   └── Generate-Architecture-Documents.ps1
├── Tests/
│   └── Decision-Test-Matrix.csv # DMN decision test matrix
└── README.md                    # This file
```

## Reports

| French (`fr/`) | English (`en/`) | Formats |
|---|---|---|
| `dossier-architecture-portage-cybertech` | `oauth2-platform-architecture` | HTML, DOCX, PDF |
| `architecture-microservices-recommandee` | `recommended-microservices-architecture` | HTML, DOCX, PDF |
| `description-processus-bpmn-oauth2` | `oauth2-bpmn-process-description` | HTML, DOCX, PDF |
| `guide-modeles-formats-techniques` | `technical-models-and-formats-guide` | HTML, DOCX, PDF |
| `note-bpel-processus-reference` | `bpel-process-reference-note` | HTML, DOCX, PDF |
| `Modèles-Pratiques-Architecture-de-Micro-Services` | `Practical-Microservices-Architectural-Patterns` | PDF |

## Architecture views

The architecture dossier covers the following views:

| View | Diagram file |
|---|---|
| Context diagram | `Power_Designer/Diagrammes/01-Architecture-contexte.png` |
| Application architecture | `Power_Designer/Diagrammes/02-Architecture-applicative.png` |
| Physical deployment | `Power_Designer/Diagrammes/03-Architecture-physique.png` |
| Component diagram | `Power_Designer/Diagrammes/04-Diagramme-composants.png` |
| Use-case diagram | `Power_Designer/Diagrammes/05-Cas-utilisation.png` |
| BPMN process | `Power_Designer/Diagrammes/06-Processus-BPMN.png` |
| OAuth 2.0 sequence | `Power_Designer/Diagrammes/07-Sequence-OAuth2.png` |
| BPEL orchestration reference | `Power_Designer/Diagrammes/08-BPEL-orchestration-reference.png` |

## Source models

| Model | File | Tool |
|---|---|---|
| BPMN process | `Power_Designer/BPMN/PortageCyberTech-OAuth2-Process.bpb` | PowerDesigner |
| UML use-cases and components | `Power_Designer/UML/PortageCyberTech-Use-Cases-and-Components.oom` | PowerDesigner |
| Enterprise application architecture | `Power_Designer/EAM/PortageCyberTech-Application-and-Physical-Architecture.eam` | PowerDesigner |
| DMN access-control decisions | `Power_Designer/DMN/PortageCyberTech-DMN-Decision-Access-Control.dmn` | DMN-compatible tool |
| Abstract BPEL orchestration | `Power_Designer/BPEL/Protected-Resource-Access-Abstract.bpel` | Reference only |
| BPEL hello flow | `Power_Designer/BPEL/ProtectedHelloFlow.bpel` | Reference only |
| Abstract service contracts | `Power_Designer/BPEL/PortageCyberTech-Abstract-Service-Contracts.wsdl` | Reference only |
| BPEL contracts | `Power_Designer/BPEL/PortageCyberTech-BPEL-Contracts.wsdl` | Reference only |

## Automation scripts

- `Scripts/Generate-PowerDesigner-Models.ps1` — regenerates native PowerDesigner
  model files and diagram images. Does **not** export the HTML/DOCX/PDF reports.
- `Scripts/Generate-Architecture-Documents.ps1` — generates architecture document
  figures and DOCX/PDF outputs from their configured HTML sources.
- `../../06_Outils-de-Generation/Convertir-HTML-en-Word-PDF.ps1` — converts the
  listed HTML reports to Word and PDF using Microsoft Word.

## Related deliverables

- Functional and technical requirements: `../01_Cadrage/fr/` and `../01_Cadrage/en/`
- Detailed software design (PlantUML diagrams): `../03_Conception/`
- Test reports and Postman collection: `../04_Tests-et-Validation/`
- Project management and deliverable register: `../05_Gestion-et-Livrables/`
- Generation tools and scripts: `../06_Outils-de-Generation/`

## Scope and caveats

The models record observed interfaces and proposed architecture. They do not
claim that a production cloud topology, API gateway, durable database,
high-availability cluster or external identity provider has been deployed.
The running Spring Boot services expose REST/JSON only. The BPEL processes
and WSDL definitions are abstract reference artifacts and require a BPEL
engine and REST adapters before they could be executed. Demo credentials,
caller-supplied subject grants, and in-memory key/client configuration are
not production defaults. Historical test results retain their original dates
and do not establish current status.


## Bilingual Postman endpoint report

The consolidated report brings together the Dev environment and historical results. French and English editions are available in HTML, Word, and PDF: [français](../04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
