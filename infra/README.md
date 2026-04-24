# Moving DebtsApp To A New VM

This repo keeps the full VM migration path in git:

- `infra/terraform/yandex/` provisions the Ubuntu VM in Yandex Cloud.
- `infra/ansible/` installs Docker + Compose plugin, then k3s, kubectl, Helm, metrics, and the app.
- `infra/k8s/` holds the rendered app, ingress, HPA, and monitoring manifests.

The Terraform VM bootstrap now creates the primary admin account as `macsia`, enables SSH key and password login for that account, and keeps direct root SSH login disabled.

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

Export a Yandex Cloud token:

```bash
export YC_TOKEN="$(yc iam create-token)"
```

## 2. Provision and bootstrap the VM

```bash
make infra-plan
make infra-apply
make render-inventory
make bootstrap-vm
```

This installs Docker, the Compose plugin, k3s, kubectl, and Helm on the new VM.

## 3. Point DNS

```bash
terraform -chdir=infra/terraform/yandex output -raw public_ip
```

Point your `A` record for `app_domain` to that IP, and also point `grafana_domain` if you keep it on a separate hostname.

## 4. Deploy the cluster workloads

```bash
make deploy-k3s
```

This deploys:

- PostgreSQL and Neo4j with persistent volumes
- backend and frontend workloads
- Traefik ingress with TLS
- backend HPA at 15% CPU target
- metrics-server if k3s does not already provide it
- Prometheus and Grafana via `kube-prometheus-stack`

App URL:

- `https://<app_domain>`

Grafana URL:

- `https://<grafana_domain>`

Load-test example:

```bash
k6 run -e BASE_URL=https://<app_domain> scripts/k6-backend-load.js
```

## 5. Remove the old host deployment

Copy `infra/ansible/inventory.example.ini` to `infra/ansible/inventory.old.ini`, fill the `old_host` entry with its own SSH key, then run:

```bash
make decommission-old-host
```

## GitHub Actions Secrets

The manual deploy workflow expects:

- `YC_TOKEN`
- `YC_CLOUD_ID`
- `YC_FOLDER_ID`
- `DEPLOY_SSH_PUBLIC_KEY`
- `DEPLOY_SSH_PRIVATE_KEY`
- `VM_ADMIN_PASSWORD`
- `VM_ADMIN_PASSWORD_HASH`
- `OLD_HOST_SSH_PRIVATE_KEY` when old-host cleanup is requested
- `GHCR_PULL_USERNAME`
- `GHCR_PULL_TOKEN`
- `POSTGRES_PASSWORD`
- `NEO4J_PASSWORD`
- `JWT_SECRET`
- `GRAFANA_ADMIN_PASSWORD`
- `BOT_TOKEN` optionally

Keep only example values in git. Real secrets stay ignored.

On this machine, the generated bootstrap credentials are stored locally under `~/.config/debtsapp/bootstrap/debtsapp-k3s/`.
