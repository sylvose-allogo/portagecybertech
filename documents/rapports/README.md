# Documentation livrée — Portage CyberTech

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** Documentation livrée — Portage CyberTech.@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-04  
> **Purpose:** Documentation livrée — Portage CyberTech.


**Projet :** Mini-plateforme OAuth 2.0 — Authorization and Resource Server Contract Tests  
**Organisation :** Portage CyberTech  
**Auteur :** Sylvose Allogo — sylvose.allogo@yahoo.com  
**Révision de classement :** 2026-10-05

**Version du projet :** 1.0.0

## Plan de classement

| Dossier | Livrables |
|---|---|
| `01_Cadrage/fr/` et `01_Cadrage/en/` | Cahier des charges fonctionnel et technique, faisabilité et périmètre en français et en anglais |
| `02_Architecture/fr/` et `02_Architecture/en/` | Architecture, microservices, BPMN, guides de modèles et note BPEL bilingues; modèles natifs et diagrammes conservés comme sources |
| `03_Conception/fr/` et `03_Conception/en/` | Dossier de conception détaillée en français et en anglais, chacun en HTML, Word et PDF |
| `03_Conception/Diagrammes-PlantUML/` | Sources PlantUML éditables des vues de conception |
| `04_Tests-et-Validation/fr/` et `04_Tests-et-Validation/en/` | Rapports de test bilingues; collections, environnements, scripts, captures et résultats historiques conservés en tant que sources/preuves |
| `05_Gestion-et-Livrables/fr/` et `05_Gestion-et-Livrables/en/` | Catalogue, référentiel des artefacts et choix technologiques en français et en anglais; matrices CSV conservées comme sources |
| `06_Outils-de-Generation/` | Scripts de génération, logo officiel Portage CyberTech et index de langue |

Les numéros indiquent l'ordre de lecture conseillé. Les documents historiques
conservent leurs dates et résultats d'origine; ils ne prouvent pas une
exécution récente. Les captures Postman brutes du dossier `Preuves/` et l'ancien rapport PDF
archivé dans `Rapports-historiques/` peuvent contenir des JWT ou des en-têtes
Basic encodés. Le nouveau rapport Word intègre des copies expurgées des
valeurs d'authentification, mais les originaux restent des preuves sensibles
à conserver dans un espace à accès contrôlé et à ne pas diffuser.

## Lecture et interprétation

1. Commencer par le cahier des charges et l'étude de faisabilité.
2. Consulter ensuite le dossier d'architecture et ses modèles natifs.
3. Consulter le rapport Postman en distinguant ses trois sources : assertions
   de la collection, captures d'écran, et rapports JUnit historiques.
4. Utiliser la matrice de traçabilité pour examiner les exigences encore
   ouvertes, notamment la nouvelle clé de signature à valider par le Resource
   Server.

Les dossiers `ci/`, `deployment/`, `.run/` et les modules Java font partie du
dépôt du projet, mais ne sont volontairement pas déplacés dans ce répertoire
de rapports. Le catalogue les recense comme références hors classement.

L'étude de faisabilité, le rapport Postman et le catalogue des livrables
peuvent être régénérés avec `06_Outils-de-Generation\Generer-Rapports.ps1`
sous Windows avec Microsoft Word installé. Les documents HTML d'architecture,
de cadrage et de gestion sont convertis avec
`06_Outils-de-Generation\Convertir-HTML-en-Word-PDF.ps1`, qui produit les
versions Word et PDF à côté de chaque source HTML. La note BPEL française et sa
traduction anglaise sont désormais des documents de référence complets dans
`02_Architecture\fr\` et `02_Architecture\en\`; elles précisent que l'exécution
BPEL n'est pas fournie par les services REST/JSON du projet.
Les rapports de cette révision incluent :

- `01_Cadrage/fr/etude-de-faisabilite-portage-cybertech.docx`
- `04_Tests-et-Validation/fr/rapport-tests-postman.docx`
- `04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html` et ses exports Word/PDF
- `04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html` et ses exports Word/PDF
- `05_Gestion-et-Livrables/fr/catalogue-livrables-ecarts.docx`
- `02_Architecture/fr/architecture-microservices-recommandee.docx`
- `02_Architecture/fr/guide-modeles-formats-techniques.docx`
- `02_Architecture/fr/note-bpel-processus-reference.html` et ses exports Word/PDF
- `02_Architecture/en/bpel-process-reference-note.html` et ses exports Word/PDF
- `02_Architecture/en/oauth2-platform-architecture.html` et ses exports Word/PDF
- `02_Architecture/en/recommended-microservices-architecture.html` et ses exports Word/PDF
- `01_Cadrage/en/functional-and-technical-requirements.html` et ses exports Word/PDF
- `04_Tests-et-Validation/fr/rapport-tests-unitaires.html` et ses exports Word/PDF
- `04_Tests-et-Validation/fr/rapport-tests-integration.html` et ses exports Word/PDF
- `04_Tests-et-Validation/en/unit-test-results.html` et ses exports Word/PDF
- `04_Tests-et-Validation/en/integration-test-results.html` et ses exports Word/PDF
- `02_Architecture/en/technical-models-and-formats-guide.html` et ses exports Word/PDF
- `05_Gestion-et-Livrables/fr/choix-technologiques-plateforme.docx`
- `05_Gestion-et-Livrables/fr/referentiel-artefacts-logiciels.docx`
- `05_Gestion-et-Livrables/en/technology-selection-for-oauth2-platform.html` et ses exports Word/PDF
- `05_Gestion-et-Livrables/en/software-artifact-register.html` et ses exports Word/PDF
- `01_Cadrage/en/project-scope-and-feasibility.html` et ses exports Word/PDF
- `04_Tests-et-Validation/en/oauth2-contract-test-report.html` et ses exports Word/PDF
- `05_Gestion-et-Livrables/en/deliverable-register-and-decisions.html` et ses exports Word/PDF
- `03_Conception/fr/conception-detaillee-mini-plateforme-oauth2.html` et ses exports Word/PDF
- `03_Conception/en/detailed-design-mini-oauth2-platform.html` et ses exports Word/PDF

Les dossiers `fr/` et `en/` des rubriques 01 à 06 séparent les livrables
documentaires par langue. Les fichiers de modèles, scripts et preuves sont
conservés à leur emplacement technique. Les deux dossiers de langue sous
`03_Conception/` contiennent la même conception :
architecture, interfaces, jetons, sécurité, déploiement, risques, tests et
traçabilité. Les paramètres de démonstration sont distingués des contrôles
recommandés pour la production. Les diagrammes sources restent dans
`Diagrammes-PlantUML/` et les modèles natifs restent sous `02_Architecture/`.
Les versions Word/PDF sont régénérées par
`06_Outils-de-Generation\Generer-Documents-Conception.ps1` avec Microsoft Word;
ce script produit également les exports Word/PDF des descriptions BPMN.

Les exports PDF des rapports appliquent les restrictions de permissions prises
en charge par le format et les lecteurs PDF compatibles. Ces restrictions ne
peuvent pas empêcher les captures d'écran, les photographies ou les outils qui
les ignorent. L'outil de protection et ses tests sont conservés dans
`integration-tests/src/test/`.

L'historique structuré des questions, décisions et réponses est livré en
français et en anglais dans `04_Tests-et-Validation/fr/` et `/en/`, au format
Word avec leurs sources HTML. La synthèse texte corrigée est
`Historique-structure-des-demandes-et-reponses-OAuth2.txt`; la source reçue
est conservée sous `Historique-source-recu-2026-10-04.txt` pour traçabilité.

Le logo SVG archivé sous `06_Outils-de-Generation/Assets/` provient du site
officiel et est utilisé sur les couvertures. Les rapports livrés portent
l'identité Portage CyberTech; les sources natives et scripts de modélisation
restent des artefacts techniques séparés.


## Rapport Postman consolidé

Le rapport unique rassemble l'environnement Dev et les résultats historiques. Les éditions française et anglaise sont disponibles en HTML, Word et PDF : [français](04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
