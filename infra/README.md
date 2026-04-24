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

Load-test example:

```bash
k6 run -e BASE_URL=https://<app_domain> scripts/k6-backend-load.js
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

Keep only example values in git. Real secrets stay ignored.
