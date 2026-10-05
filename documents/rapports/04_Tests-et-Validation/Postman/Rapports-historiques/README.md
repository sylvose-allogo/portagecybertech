# Contract-test guidance and historical results

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** Contract-test guidance and historical results.o.com · **Version:** 1.0.0 · **Date:** 2026-10-04  
> **Purpose:** Contract-test guidance and historical results.


**Project:** Portage CyberTech OAuth 2.0 · **Project version:** 1.0.0  
**Document revision:** 1.1 · **Date:** 2026-10-05  
**Author:** Sylvose Allogo · sylvose.allogo@yahoo.com

This folder documents the OAuth 2.0/JWT contract checks and contains generated
HTML/JUnit evidence from specific prior runs. Reported counts and dates apply
only to the execution recorded inside each file; they must not be read as
evidence that the current checkout has just passed the same tests.

## Contract boundaries

| Producer / consumer | Contract to verify | Principal failure conditions |
|---|---|---|
| OAuth client → Authorization Server | HTTP Basic client authentication; form-encoded `POST /oauth2/token`; RFC 8693 source and requested token types; OAuth JSON responses | Invalid client, unsupported grant, malformed request, invalid source token, unapproved audience, unauthorised scope |
| Resource Server → Authorization Server | HTTP GET to the configured JWKS URI; public RSA keys selected by JWT `kid` | Unreachable or empty JWKS, absent/unusable public key, incorrect signature |
| OAuth client → Resource Server | Bearer JWT on `GET /api/hello`; correct issuer, expiration, audience and `api.read` scope | Missing or invalid token (401), valid token without required authority (403) |
| Operations | Public `GET /actuator/health` | Application reports non-healthy or endpoint cannot be reached |

The original functional scope requires a signed JWT issuer and a secured
HelloWorld endpoint; the current repository adds an RFC 8693 exchange and
explicit API audience/scope validation. It has no implemented `PUT`, `PATCH`
or `DELETE` API route.

## Focused local tests

From the repository root, run the project-wide suite:

```powershell
mvn test
```

Run service-local suites independently when investigating one boundary:

```powershell
mvn -pl authorization-server test
mvn -pl resource-server test
```

The cross-service tests in `integration-tests` start Spring Boot processes
and send HTTP requests. They are test-scope dependencies and are not a
production service-to-service library. Tests use generated/controlled keys
and isolated local JWKS fixtures where appropriate.

The Cucumber feature in
`integration-tests/src/test/resources/features/miniPlateformeOAuth.feature`
states high-level token-issuance and public-JWKS scenarios. The detailed Java
tests assert client rejection, token claims, token/JWKS consistency,
unauthenticated API rejection, invalid signatures and missing required scope.

## Result artifacts and freshness

| Artifact | Meaning |
|---|---|
| `unit-tests.html` | Historical HTML summary; inspect its title and run date |
| `integration-tests.html` | Historical HTML summary; inspect its title and run date |
| `postman-Env-Test.xml`, `postman-Env-Int.xml`, `postman-Env-Prod.xml` | Stored Postman JUnit XML results for their recorded localhost simulations |

Reports are retained as execution evidence. Do not edit their historical
counts or dates to match a later run. Save a new report or rerun Maven/Postman
to establish the status of a changed checkout.

Branded, bilingual reading copies of the two Maven HTML summaries, with Word
and PDF exports, are available as `../fr/rapport-tests-unitaires` and
`../en/unit-test-results`, and as `../fr/rapport-tests-integration` and
`../en/integration-test-results`. The original generated HTML files and XML
evidence in this directory remain unchanged.

The consolidated Postman endpoint report is available in French as
`../../fr/Rapport-des-tests-Endpoint-Postman-Env-Dev` and in English as
`../../en/Postman-Endpoint-Test-Report-Dev-Environment`, each in HTML, Word,
and PDF. It combines the collection context, the Dev profile, historical
JUnit counts, and the supplied Postman execution evidence.

## Security and simulation limitations

The subject-demo grant accepts a caller-supplied subject and does not
authenticate it. The Authorization Server's checked-in
`OAUTH_SUBJECT_GRANT_ENABLED` default is `true`; tests or local profile files
may explicitly choose their own value. Set it to `false` in every shared or
production environment. The checked-in client secret is a known development
value and is never a deployment secret.

The Resource Server integration tests use a local HTTP JWKS fixture to test a
real HTTP key-resolution boundary without trusting an unrelated external
identity provider. Historical Postman profiles named Int/Prod may still point
at localhost; profile names do not make them real deployment environments.

## Architecture and acceptance criteria

See the detailed French-language requirements and acceptance checklist in
[`../../01_Cadrage/fr/cahier-des-charges-fonctionnel-technique.pdf`](../../01_Cadrage/fr/cahier-des-charges-fonctionnel-technique.pdf)
and the corresponding Word source. The CSV decision-test matrix and DMN
decision tables in
[`../../02_Architecture/`](../../02_Architecture/README.md) distinguish HTTP
authentication failures from token-exchange policy failures and Resource
Server authorization failures.


## Bilingual Postman endpoint report

The consolidated report brings together the Dev environment and historical results. French and English editions are available in HTML, Word, and PDF: [français](../../fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../../en/Postman-Endpoint-Test-Report-Dev-Environment.html).
