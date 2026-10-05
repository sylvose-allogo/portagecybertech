# Server deployment with Docker Compose

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** Server deployment with Docker Compose.ogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-04  
> **Purpose:** Server deployment with Docker Compose.


This directory deploys the two runnable Spring Boot services as separate,
non-root containers. `integration-tests` is a test-only Maven module and is
intentionally not deployed. The Compose network is private to this application;
only the configured HTTP ports are published on the host.

## Build and start

Requirements: Docker Engine with the Compose v2 plugin and outbound access to
the Maven and container-image repositories on the first build.

The following Dockerfiles are provided:
- `authorization-server.Dockerfile` — builds the Authorization Server image.
- `resource-server.Dockerfile` — builds the Resource Server image.

From the repository root:

```powershell
Copy-Item deployment\.env.example deployment\.env
# Edit deployment\.env and replace OAUTH_CLIENT_SECRET with a unique value.
docker compose --env-file deployment\.env -f deployment\compose.yaml config
docker compose --env-file deployment\.env -f deployment\compose.yaml up --build -d
docker compose --env-file deployment\.env -f deployment\compose.yaml ps
```

The default host binding is loopback-only (`127.0.0.1`). Keep it that way for a
single-server local demonstration. For a remote host, bind only behind an
approved TLS-terminating reverse proxy and firewall; do not expose the
unencrypted application ports directly to the public internet.

## Service configuration

The Authorization Server is available on host port `9090`; the Resource Server
is available on `8080`. Set `AUTHORIZATION_SERVER_HOST_PORT`,
`RESOURCE_SERVER_HOST_PORT`, and `BIND_ADDRESS` in the ignored `.env` file to
adapt those bindings. The default issuer, `http://authorization-server:9090`,
is the internal Compose service name and must be kept identical for both
services. If selecting another issuer, it must resolve and be reachable from
both containers, and the issued `iss` value must exactly match the value the
Resource Server validates. `OAUTH_API_AUDIENCE` must match the audience
requested and accepted by the API.

The deployment explicitly disables the caller-supplied-subject grant, reduces
health-detail exposure and web logging, enables template caching, drops Linux
capabilities, uses a read-only root filesystem, and rejects privilege
escalation. The signing key and registered client repository are still
in-memory demo implementations: this Compose setup does not add persistent
key management, durable client storage, TLS termination, backups, monitoring,
or production certification.

Use `docker compose --env-file deployment\.env -f deployment\compose.yaml logs -f`
for service logs and the same command with `down` to stop the stack. Never
commit `.env`, client secrets, private keys, or server credentials.


## Bilingual Postman endpoint report

The consolidated report brings together the Dev environment and historical results. French and English editions are available in HTML, Word, and PDF: [français](../documents/rapports/04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../documents/rapports/04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
