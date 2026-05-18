# Deploying DebtsApp To A Yandex Cloud VM

This repo keeps the full VM deploy path in git:

- `infra/terraform/yandex/` provisions the Ubuntu VM in Yandex Cloud.
- `infra/ansible/` installs k3s, kubectl, Helm, and the application via `site.yml`.
- `infra/k8s/` holds the rendered app, ingress, HPA, and monitoring manifests.

The Terraform VM bootstrap creates the primary admin account as `macsia`, enables SSH key and password login for that account, and keeps direct root SSH login disabled.

## 1. Prepare repo-owned config

```bash
cp infra/terraform/yandex/terraform.tfvars.example infra/terraform/yandex/terraform.tfvars
cp infra/ansible/vars/deploy-secrets.example.yml infra/ansible/vars/deploy-secrets.yml
```

Edit:

- `infra/terraform/yandex/terraform.tfvars`
- `infra/ansible/group_vars/all.yml`
- `infra/ansible/vars/deploy-secrets.yml`

Terraform uses the repo-local mirror config in `infra/terraform/terraformrc`, so start with:

```bash
make infra-init
```

Export a Yandex Cloud OAuth token:

```bash
export YC_TOKEN="<your OAuth token>"
export YC_FOLDER_ID="<folder id>"
```

## 2. Provision the VM

```bash
make infra-plan
make infra-apply
```

## 3. Render inventory and deploy everything

```bash
make render-inventory
make deploy
```

`make deploy` runs the single site playbook `infra/ansible/site.yml`, which bootstraps k3s on the VM and then rolls out the application, ingress, TLS, and monitoring.

## 4. Point DNS

```bash
terraform -chdir=infra/terraform/yandex output -raw public_ip
```

Point your `A` record for `app_domain` to that IP, and also point `grafana_domain` if you keep it on a separate hostname.

The `make deploy` run configures:

- PostgreSQL and Neo4j with persistent volumes
- backend and frontend workloads
- Traefik ingress with TLS via cert-manager
- backend HPA at 15% CPU target
- metrics-server
- Prometheus and Grafana via `kube-prometheus-stack`

App URL: `https://<app_domain>`. Grafana URL: `https://<grafana_domain>`.

Grafana login:

- user: `grafana`
- password: generated locally in ignored `local-passwords.env` and stored in the GitHub Actions secret `GRAFANA_ADMIN_PASSWORD`

The deploy playbook also performs HTTPS smoke checks through Traefik for the frontend, backend health endpoint, and Grafana. The GitHub deploy workflow repeats those checks from the hosted runner and validates Grafana `/api/user` with the configured credentials, so broken DNS, firewall, TLS, public ingress routing, or Grafana auth fails the deployment.

Check Kubernetes routing targets:

```bash
kubectl get pods -n debtsapp -o wide
kubectl get hpa -n debtsapp
kubectl get svc,endpoints -n debtsapp
kubectl get servicemonitor -n debtsapp
kubectl get certificate,challenge,order -A
```

Load-test example:

```bash
k6 run -e BASE_URL=https://<app_domain> scripts/k6-backend-load.js
```

The preferred Lab 3 load test path is the manual GitHub Actions workflow `Load Test: backend HPA`. On branches where GitHub cannot dispatch a newly added workflow file yet, run the existing manual deploy workflow with `operation=load_test`. Both paths run the same k6 script from the official `grafana/k6` Docker image and upload `load-summary.json` plus `hpa-evidence.log`. The optional `k6_stages` input accepts comma-separated `duration:target` pairs, for example `30s:10,60s:40,60s:80,30s:0`.

If Traefik shows `no available server`, check that `debts-frontend` has ready endpoints:

```bash
kubectl get endpoints debts-frontend -n debtsapp
kubectl describe deployment debts-frontend -n debtsapp
kubectl get events -n debtsapp --sort-by=.lastTimestamp | tail -n 40
```

## GitHub Actions Secrets

The manual deploy workflow expects:

- `YC_TOKEN`
- `YC_CLOUD_ID`
- `YC_FOLDER_ID`
- `DEPLOY_SSH_PUBLIC_KEY`
- `DEPLOY_SSH_PRIVATE_KEY`
- `VM_ADMIN_PASSWORD_HASH`
- `GHCR_PULL_USERNAME`
- `GHCR_PULL_TOKEN`
- `POSTGRES_PASSWORD`
- `NEO4J_PASSWORD`
- `JWT_SECRET`
- `GRAFANA_ADMIN_PASSWORD`
- `BOT_TOKEN` optionally

CI/CD (Lab 4) additionally uses:

- `SONAR_TOKEN` — SonarCloud user token ([setup guide](../docs/sonarcloud-setup.md))
- Repository variable `SONARQUBE_URL` = `https://sonarcloud.io`
- `TELEGRAM_CHAT_IDS` — comma-separated Telegram chat IDs for pipeline notifications
- `ARGOCD_AUTH_TOKEN` — Argo CD API token for post-deploy sync verification on `lab4`
- `ARGOCD_SERVER` (optional) — defaults to `argocd.app.macsia.fun`
- `APP_DOMAIN` (optional) — defaults to `debtsapp.app.macsia.fun`

Keep only example values in git. Real secrets stay ignored.

## Lab 4: SonarCloud, Argo CD CD, Telegram

### SonarCloud (CI)

The `CI: сборка и тесты` workflow runs a dedicated `sonarqube-scan` job against [SonarCloud](https://sonarcloud.io). The pipeline fails when:

- unit tests or JaCoCo/Jest coverage drop below **80%**
- the SonarCloud Quality Gate fails (coverage, bugs, vulnerabilities, hotspots)

See [docs/sonarcloud-setup.md](../docs/sonarcloud-setup.md) for Quality Gate configuration on sonarcloud.io.

### Argo CD continuous delivery

After images are published to GHCR on branch `lab4`, CI commits new image tags to `infra/helm/debtsapp-app/values.yaml`. Argo CD Application `debtsapp` (installed by Ansible from `infra/k8s/92-argocd-app.yaml.j2`) syncs the Helm chart automatically. Job `argocd-verify` waits for `Healthy` + `Synced` and checks `https://<app_domain>/api/actuator/health`.

Initial cluster bootstrap remains the manual **Deploy: VM и приложение** workflow; subsequent releases are GitOps-only.

### Telegram CI/CD bot

Create a bot via [@BotFather](https://t.me/BotFather), store the token in `BOT_TOKEN`, and add your chat ID(s) to `TELEGRAM_CHAT_IDS` (comma-separated). The reusable action `.github/actions/notify-telegram` sends per-job alerts on failure and a final pipeline summary.
