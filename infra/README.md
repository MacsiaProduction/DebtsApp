# Deploying DebtsApp To A Yandex Cloud VM

This repo keeps the full lab-2 VM deploy path in git:

- [`terraform/yandex/`](terraform/yandex) provisions the Ubuntu VM in Yandex Cloud.
- [`ansible/`](ansible) installs Docker and Docker Compose, copies the repo to the VM, and starts the application stack.
- [`../docker-compose.yml`](../docker-compose.yml) defines Caddy, PostgreSQL, Neo4j, backend, and frontend services.
- [`caddy/Caddyfile`](caddy/Caddyfile) configures HTTPS for `debtsapp2.macsia.fun`.

The Terraform VM bootstrap creates the primary admin account as `macsia`, enables SSH key and password login for that account, and keeps direct root SSH login disabled.

## 1. Prepare repo-owned config

```bash
cp infra/terraform/yandex/terraform.tfvars.example infra/terraform/yandex/terraform.tfvars
cp infra/ansible/vars/deploy-secrets.example.yml infra/ansible/vars/deploy-secrets.yml
```

Edit:

- `infra/terraform/yandex/terraform.tfvars`
- `infra/ansible/vars/deploy-secrets.yml`

Set `app_domain` to `debtsapp2.macsia.fun` and point its DNS `A` record to the VM public IP before expecting HTTPS issuance to succeed. The current lab-2 VM IP is `37.230.169.243`.

Terraform uses the repo-local mirror config in [`infra/terraform/terraformrc`](infra/terraform/terraformrc), so start with:

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

The GitHub deploy workflow runs Terraform from a fresh runner. If Yandex Cloud reports that the VM already exists, [`scripts/terraform_apply.sh`](../scripts/terraform_apply.sh) treats that as non-fatal and deploys to the existing VM resolved by name. For larger environments, move Terraform state to a remote backend instead of relying on this lab shortcut.

## 3. Render inventory and deploy everything

```bash
make render-inventory
make deploy
```

[`make deploy`](../Makefile) runs [`ansible/site.yml`](ansible/site.yml), which installs Docker on the VM, synchronizes the repository, writes the deployment `.env`, and starts the stack with Docker Compose.

## 4. DNS and HTTPS

Get the VM public IP:

```bash
terraform -chdir=infra/terraform/yandex output -raw public_ip
```

Then:

- point `debtsapp2.macsia.fun` and `portainer.macsia.fun` to that IP
- ensure inbound ports `80` and `443` are reachable from the internet
- rerun [`make deploy`](../Makefile) if DNS was not ready during the first certificate attempt

Caddy will automatically obtain and renew Let's Encrypt certificates for `debtsapp2.macsia.fun` and `portainer.macsia.fun`.

## 5. Access the application

- App: `https://debtsapp2.macsia.fun`
- Portainer: `https://portainer.macsia.fun`
- PostgreSQL: `37.230.169.243:5432`
- Neo4j Browser: `http://37.230.169.243:7474`
- Neo4j Bolt: `37.230.169.243:7687`

## Lab-2 Verification Checklist

- Terraform provisions the VM from [`terraform/yandex/main.tf`](terraform/yandex/main.tf).
- Ansible installs Docker and starts Compose from [`ansible/deploy-docker-compose.yml`](ansible/deploy-docker-compose.yml).
- Compose contains backend, frontend, PostgreSQL, Neo4j, Caddy, and Portainer services.
- CI publishes Docker images to GHCR as `ghcr.io/macsiaproduction/debtsapp-backend:lab2` and `ghcr.io/macsiaproduction/debtsapp-frontend:lab2`.
- The latest confirmed successful publish run is `https://github.com/MacsiaProduction/DebtsApp/actions/runs/25138164224`.
- After deploy, verify:

```bash
curl -I https://debtsapp2.macsia.fun
curl https://debtsapp2.macsia.fun/api/actuator/health
curl -I https://portainer.macsia.fun
```

## GitHub Actions Secrets

The manual deploy workflow expects:

- `YC_TOKEN`
- `YC_CLOUD_ID`
- `YC_FOLDER_ID`
- `DEPLOY_SSH_PUBLIC_KEY`
- `DEPLOY_SSH_PRIVATE_KEY`
- `VM_ADMIN_PASSWORD_HASH`
- `POSTGRES_PASSWORD`
- `NEO4J_PASSWORD`
- `JWT_SECRET`
- `BOT_TOKEN` optionally

Workflow defaults:

- VM name: `debtsapp-docker`
- App domain: `debtsapp2.macsia.fun`
- HTTPS email: `macsia.production@gmail.com`

Keep only example values in git. Real secrets stay ignored.
