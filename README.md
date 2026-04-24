# Debt Calculation Network

DebtsApp is a monorepo with a Spring Boot backend, a React frontend, and repo-owned infrastructure for lab-2 deployment to a VM.

## Canonical Deployment Path

The maintained deployment path is:

`Terraform (Yandex VM) -> Ansible install Docker -> Docker Compose -> Caddy HTTPS`

Use [infra/README.md](infra/README.md) when deploying the app to a VM.

## Repo Layout

```text
backend/              Spring Boot API
frontend/             React app
infra/                Terraform + Ansible deployment + Caddy config
docker-compose.yml    Local and VM Docker Compose stack
```

## Local And Lab-2 Use

[`docker-compose.yml`](docker-compose.yml) and both Dockerfiles are the main deployment path for lab 2.

Useful commands:

```bash
make test-backend
make test-backend-integration
make docker-up
make docker-down
make infra-apply
make render-inventory
make deploy
```

## Runtime Notes

- Public app URL is [`https://debtsapp2.macsia.fun`](README.md:40).
- HTTPS is terminated by [`Caddy`](infra/caddy/Caddyfile:1), which automatically requests and renews Let's Encrypt certificates.
- Frontend and backend are served behind the same domain; [`/api`](frontend/nginx.conf:8) is proxied to the backend.
- PostgreSQL is available on port `5432`.
- Neo4j is available on ports `7474` and `7687`.
- Backend unit tests run with `./gradlew test`; Docker-backed persistence coverage runs with `./gradlew integrationTest`.
