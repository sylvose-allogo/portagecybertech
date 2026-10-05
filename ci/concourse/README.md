# Isolated Concourse CI

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** Isolated Concourse CI.l:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-04  
> **Purpose:** Isolated Concourse CI.


This standalone Compose project runs a Concourse web node, a PostgreSQL
database, and a dedicated worker on its own network and named volumes. The UI
binds to `127.0.0.1:8082`; the worker requires privileged container execution,
so run this local demonstration only on a disposable, dedicated Linux VM or
equivalent isolated Docker host. Do not share that worker with production
workloads.

## Files

| File / directory | Purpose |
|---|---|
| `compose.yaml` | Compose service definition (web, PostgreSQL, worker) |
| `pipeline.yaml` | Declarative Concourse pipeline definition |
| `vars.example.yaml` | Example pipeline variable values (copy and customise locally) |
| `tasks/` | Concourse task definitions referenced by `pipeline.yaml` |
| `.env.example` | Template for the required `.env` secrets file (never commit `.env` or `keys/`) |

## Prepare and start

From this directory:

1. Copy `.env.example` to `.env`; replace every sample password with a unique
   random value.
2. Generate the local SSH keys in a protected `keys/` directory:

   ```sh
   mkdir -p keys
   ssh-keygen -t rsa -b 4096 -N '' -f keys/tsa_host_key
   ssh-keygen -t rsa -b 4096 -N '' -f keys/worker_key
   cp keys/worker_key.pub keys/authorized_worker_keys
   ssh-keygen -t rsa -b 4096 -m PEM -N '' -f keys/session_signing_key
   ```

   Concourse uses the session signing file as private signing material; use a
   dedicated generated RSA key and keep the complete directory out of source
   control.
3. Validate and start the services:

   ```sh
   docker compose --env-file .env config
   docker compose --env-file .env up -d
   ```

4. Open `http://localhost:8082`, then install `pipeline.yaml` using the
   committed example variables:

   ```sh
   fly -t local login -c http://localhost:8082 -u "$CONCOURSE_LOCAL_USER" -p "$CONCOURSE_LOCAL_PASSWORD"
   fly -t local set-pipeline -p portagecybertech -c pipeline.yaml -l vars.example.yaml
   fly -t local unpause-pipeline -p portagecybertech
   ```

The Git resource is configured for the public repository URL declared in the
project POM. For another or private remote, change `git-url`/`git-branch` in a
private vars file and supply repository credentials using Concourse credential
management; do not commit access tokens or private keys. Build output JARs are
available as the task artifact `executable-jars`. The pipeline does not deploy
or publish those artifacts.

## Isolation and lifecycle

The Concourse stack is independent from Jenkins and the service deployment.
Its `.env`, generated `keys/`, database volume, and worker volume contain
credentials or state and must remain private. The worker runs privileged to
support Concourse task containers; do not run this Compose file on a shared
production host. Stop it with `docker compose --env-file .env down`; preserve
named volumes unless intentionally destroying CI state.


## Bilingual Postman endpoint report

The consolidated report brings together the Dev environment and historical results. French and English editions are available in HTML, Word, and PDF: [français](../../documents/rapports/04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../../documents/rapports/04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
