# Isolated Jenkins and Configuration as Code

> **Organization:** Portage Cybertech · **Project:** Mini OAuth 2.0 platform  
> **Author:** Sylvose Allogo · **Email:** sylvose.allogo@yahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-05  
> **Purpose:** Isolated Jenkins and Configuration as Code.ahoo.com · **Version:** 1.0.0 · **Date:** 2026-10-04  
> **Purpose:** Isolated Jenkins and Configuration as Code.


The Compose file creates a dedicated Jenkins controller, a private project
network, a persistent named home volume, and a loopback-only web port (`8081`).
JCasC provisions the local administrator, read-only authenticated access,
controller executor isolation, and the reactor pipeline job. The job obtains
its `Jenkinsfile` from the configured Git repository.

## Files

| File / directory | Purpose |
|---|---|
| `Dockerfile` | Builds the Jenkins controller image with pre-installed plugins |
| `plugins.txt` | List of Jenkins plugins installed at image build time |
| `compose.yaml` | Compose service definition (controller, network, named volume) |
| `Jenkinsfile` | Declarative pipeline executed by the build agent |
| `casc/` | JCasC YAML files mounted read-only into the controller |
| `.env.example` | Template for the required `.env` secrets file (never commit `.env`) |

## Start

1. Copy `.env.example` to `.env` and set a unique administrator password.
2. Confirm `GIT_REPOSITORY_URL` and `GIT_BRANCH` point to the intended remote
   repository and branch.
3. From this directory run `docker compose --env-file .env config`, then
   `docker compose --env-file .env up --build -d`.
4. Open `http://localhost:8081` and connect a dedicated Linux build agent with
   the label `java23`. That agent needs JDK 23, Maven 3.9+, and access to this
   repository. The controller intentionally has zero build executors.

The checked-in JCasC YAML is mounted read-only. The admin password is supplied
through an untracked `.env`; for a shared CI service use the platform's secret
store instead. If the Git repository is private, add a narrowly scoped
read-only SCM credential in Jenkins and configure it on the generated job.
Never mount the host Docker socket into the controller or use production
credentials on an untrusted build agent.

## Isolation and operations

This controller is a separate Compose project/network from the application
deployment and from Concourse. It does not expose Jenkins on non-loopback
interfaces, deploy applications, or publish container images. A separate
Java 23 agent is required to run builds; do not enable controller executors
to work around a missing agent. Protect and back up the named
`jenkins-home` volume. Stop with `docker compose --env-file .env down`; add
`-v` only when intentionally deleting the controller's persisted state.


## Bilingual Postman endpoint report

The consolidated report brings together the Dev environment and historical results. French and English editions are available in HTML, Word, and PDF: [français](../../documents/rapports/04_Tests-et-Validation/fr/Rapport-des-tests-Endpoint-Postman-Env-Dev.html), [English](../../documents/rapports/04_Tests-et-Validation/en/Postman-Endpoint-Test-Report-Dev-Environment.html).
