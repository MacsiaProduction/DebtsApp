# Deploying DebtsApp To A Yandex Cloud VM

This repo keeps the full lab-2 VM deploy path in git:

- [`infra/terraform/yandex/`](infra/terraform/yandex) provisions the Ubuntu VM in Yandex Cloud.
- [`infra/ansible/`](infra/ansible) installs Docker and Docker Compose, copies the repo to the VM, and starts the application stack.
- [`docker-compose.yml`](docker-compose.yml) defines Caddy, PostgreSQL, Neo4j, backend, and frontend services.
- [`infra/caddy/Caddyfile`](infra/caddy/Caddyfile) configures HTTPS for [`debtsapp2.macsia.fun`](infra/README.md:7).

The Terraform VM bootstrap creates the primary admin account as `macsia`, enables SSH key and password login for that account, and keeps direct root SSH login disabled.

## 1. Prepare repo-owned config

```bash
cp infra/terraform/yandex/terraform.tfvars.example infra/terraform/yandex/terraform.tfvars
cp infra/ansible/vars/deploy-secrets.example.yml infra/ansible/vars/deploy-secrets.yml
```

Edit:

- [`infra/terraform/yandex/terraform.tfvars`](infra/terraform/yandex/terraform.tfvars)
- [`infra/ansible/vars/deploy-secrets.yml`](infra/ansible/vars/deploy-secrets.yml)

Set [`app_domain`](infra/terraform/yandex/terraform.tfvars.example:9) to `debtsapp2.macsia.fun` and point its DNS `A` record to the VM public IP before expecting HTTPS issuance to succeed.

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

## 3. Render inventory and deploy everything

```bash
make render-inventory
make deploy
```

[`make deploy`](../Makefile) runs [`infra/ansible/site.yml`](infra/ansible/site.yml), which installs Docker on the VM, synchronizes the repository, writes the deployment [`.env`](infra/ansible/deploy-docker-compose.yml:104), and starts the stack with Docker Compose.

## 4. DNS and HTTPS

Get the VM public IP:

```bash
terraform -chdir=infra/terraform/yandex output -raw public_ip
```

Then:

- point `debtsapp2.macsia.fun` to that IP
- ensure inbound ports `80` and `443` are reachable from the internet
- rerun [`make deploy`](../Makefile) if DNS was not ready during the first certificate attempt

Caddy will automatically obtain and renew the Let's Encrypt certificate for [`debtsapp2.macsia.fun`](infra/caddy/Caddyfile:5).

## 5. Access the application

- App: `https://debtsapp2.macsia.fun`
- PostgreSQL: `<public_ip>:5432`
- Neo4j Browser: `http://<public_ip>:7474`
- Neo4j Bolt: `<public_ip>:7687`

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
