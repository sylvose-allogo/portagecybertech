# Documentation de conception

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** Documentation de conception.ylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-04  
> **Purpose:** Documentation de conception.


La conception détaillée est livrée en deux langues et formats :

| Langue | Source HTML | Exports |
|---|---|---|
| Français | `fr/conception-detaillee-mini-plateforme-oauth2.html` | Word et PDF dans `fr/` |
| English | `en/detailed-design-mini-oauth2-platform.html` | Word and PDF in `en/` |

Les six sources PlantUML éditables sont conservées dans
`Diagrammes-PlantUML/` lorsqu'elles sont présentes. Les diagrammes sont
référencés par la documentation sans déplacer ni modifier les sources
techniques.

Pour régénérer les exports avec Microsoft Word, exécuter depuis la racine du
dépôt :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\documents\rapports\06_Outils-de-Generation\Generer-Documents-Conception.ps1
```


## Rapport Postman consolidé

Le rapport unique rassemble l'environnement Dev et les résultats historiques. Les éditions française et anglaise sont disponibles en HTML, Word et PDF : [français](../04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
