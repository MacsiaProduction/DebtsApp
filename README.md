# Debt Calculation Network

DebtsApp is a monorepo with a Spring Boot backend, a React frontend, and repo-owned infrastructure for moving the app to a new VM.

## Canonical Deployment Path

The maintained production path is:

`Terraform (Yandex VM) -> Ansible bootstrap -> single-node k3s -> Traefik HTTPS ingress`

Use [infra/README.md](infra/README.md) when deploying the app to a VM.

## Repo Layout

```text
backend/              Spring Boot API
frontend/             React app
infra/                Terraform, Ansible, Kubernetes, monitoring
docker-compose.yml    Local/lab-2 stack only
```

## Local And Lab Use

`docker-compose.yml` and both Dockerfiles stay in the repo for local development and lab-2 evidence. They are not the maintained VM production deployment path.

Useful commands:

```bash
make test-backend
make test-backend-integration
make docker-up
make infra-apply
make render-inventory
make deploy
```

## Runtime Notes

- Public app traffic is served through Traefik ingress on `https://<app_domain>`.
- Frontend traffic goes to `/`, backend traffic goes to `/api`.
- Backend health and metrics endpoints stay available for probes and Prometheus.
- Lab 3 deploys must prove public HTTPS access for the frontend, backend health endpoint, and Grafana before the workflow is considered successful.
- Grafana is deployed with the `grafana` admin user; the generated local password is stored in ignored `local-passwords.env`.
- Lab 3 load testing is available as the manual `Load Test: backend HPA` GitHub Actions workflow.
- Backend unit tests run with `./gradlew test`; Docker-backed persistence coverage runs with `./gradlew integrationTest`.
- Lab 4 CI uses SonarCloud static analysis, enforces **80%** test coverage, and publishes GitOps state for Argo CD on `lab4`. Argo CD manages the shared bootstrap resources, the DebtsApp workloads, monitoring, and Portainer after the initial bootstrap deploy. See [docs/sonarcloud-setup.md](docs/sonarcloud-setup.md) and [infra/README.md](infra/README.md#lab-4-sonarcloud-argo-cd-cd-telegram).
