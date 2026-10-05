# Architecture OAuth 2.0 — français


> **Organisation :** Portage Cybertech · **Projet :** Mini plateforme OAuth 2.0  
> **Auteur :** Sylvose Allogo · **Courriel :** sylvose.allogo@yahoo.com · **Version :** 1.0.0 · **Date :** 2026-10-05  
> **Objet :** Architecture OAuth 2.0 — livrables en français.


Ce répertoire contient l'édition française de tous les livrables d'architecture
de la mini plateforme OAuth 2.0 de Portage CyberTech. Chaque document est
disponible en HTML, Word (DOCX) et PDF. Les équivalents anglais se trouvent
dans `../en/`.

## Documents

| Nom de base | Description |
|---|---|
| `dossier-architecture-portage-cybertech` | Dossier d'architecture principal : vues de contexte, architecture applicative, composants, déploiement physique, séquence OAuth 2.0, journal de décisions et traçabilité des exigences. |
| `architecture-microservices-recommandee` | Architecture microservices recommandée : patrons de conception, frontières de sécurité, options de stockage des secrets et limites de la mise en œuvre actuelle en production. |
| `description-processus-bpmn-oauth2` | Description détaillée du processus BPMN : obtention d'un jeton, échange de jeton (RFC 8693) et accès à une ressource protégée. |
| `guide-modeles-formats-techniques` | Guide des notations de modélisation utilisées : BPMN, UML, EAM (Enterprise Application Mapping), DMN, BPEL/WSDL et formats connexes. |
| `note-bpel-processus-reference` | Note d'orchestration BPEL conceptuelle pour l'échange de jeton et l'appel d'une API protégée ; nécessite un moteur BPEL et des adaptateurs REST pour être exécuté. |
| `Modèles-Pratiques-Architecture-de-Micro-Services` | Livre de référence : *Modèles Pratiques d'Architecture de Micro Services — Microservices Java événementiels avec Spring Boot et Spring Cloud*. |

## Vues d'architecture couvertes

Le dossier principal (`dossier-architecture-portage-cybertech`) comprend les
vues suivantes, issues des diagrammes PowerDesigner dans
`../Power_Designer/Diagrammes/` :

| Vue | Diagramme source |
|---|---|
| Diagramme de contexte | `01-Architecture-contexte.png` |
| Architecture applicative | `02-Architecture-applicative.png` |
| Déploiement physique | `03-Architecture-physique.png` |
| Diagramme de composants | `04-Diagramme-composants.png` |
| Diagramme de cas d'utilisation | `05-Cas-utilisation.png` |
| Processus BPMN des jetons | `06-Processus-BPMN.png` |
| Séquence OAuth 2.0 | `07-Sequence-OAuth2.png` |
| Référence d'orchestration BPEL | `08-BPEL-orchestration-reference.png` |

## Livrables connexes

- **Cahier des charges fonctionnel et technique :** `../../01_Cadrage/fr/cahier-des-charges-fonctionnel-technique` (HTML, DOCX, PDF)
- **Conception détaillée du logiciel :** `../../03_Conception/fr/` (HTML, DOCX, PDF)
- **Modèles sources et diagrammes :** `../Power_Designer/` (BPMN, UML, EAM, DMN, BPEL)
- **Rapports de tests et collection Postman :** `../../04_Tests-et-Validation/fr/`
- **Édition anglaise de ces rapports :** `../en/`

## Régénération des documents

- `../Scripts/Generate-PowerDesigner-Models.ps1` — régénère les modèles natifs
  PowerDesigner et les images de diagrammes. Ne produit **pas** les rapports de
  ce répertoire.
- `../Scripts/Generate-Architecture-Documents.ps1` — génère les figures des
  documents et les sorties DOCX/PDF à partir des sources HTML configurées.
- `../../06_Outils-de-Generation/Convertir-HTML-en-Word-PDF.ps1` — convertit les
  rapports HTML en Word et PDF via Microsoft Word.

## Périmètre et réserves

Les rapports d'architecture décrivent les interfaces observées et la conception
proposée. Ils n'affirment pas qu'une topologie cloud de production, une
passerelle API, une base de données persistante, un cluster haute disponibilité
ou un fournisseur d'identité externe ont été déployés. Les services Spring Boot
livrés exposent uniquement du REST/JSON. Les processus BPEL et les définitions
WSDL sont des artefacts de référence abstraits et nécessitent un moteur BPEL
ainsi que des adaptateurs REST pour être exécutés. Les identifiants de
démonstration, les grants à sujet fourni par l'appelant et la configuration
en mémoire des clés et des clients ne sont pas des valeurs par défaut de
production. Les résultats de tests historiques conservent leur date d'origine
et n'établissent pas l'état actuel.


## Rapport Postman consolidé

Le rapport unique sur les tests des endpoints rassemble l'environnement Dev et les résultats historiques. Les éditions française et anglaise sont disponibles en HTML, Word et PDF : [français](../../04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../../04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
