TERRAFORM_DIR := infra/terraform/yandex
ANSIBLE_DIR := infra/ansible
K3S_INVENTORY ?= $(ANSIBLE_DIR)/inventory.ini

.PHONY: help build-backend test-backend test-backend-integration run-backend clean-backend \
        docker-up docker-down infra-init infra-plan infra-apply render-inventory deploy

help:
	@echo "Available commands:"
	@echo "  make build-backend    - Build the backend application"
	@echo "  make test-backend     - Run backend unit/web tests"
	@echo "  make test-backend-integration - Run backend Docker-backed integration tests"
	@echo "  make run-backend      - Run the backend application"
	@echo "  make clean-backend    - Clean backend build artifacts"
	@echo "  make docker-up        - Start the local lab-2 Docker Compose stack"
	@echo "  make docker-down      - Stop the local lab-2 Docker Compose stack"
	@echo "  make infra-init       - Initialize Terraform for the new VM"
	@echo "  make infra-plan       - Preview Terraform changes for the new VM"
	@echo "  make infra-apply      - Provision or update the new VM"
	@echo "  make render-inventory - Render Ansible inventory from Terraform outputs"
	@echo "  make deploy           - Run the full site playbook (bootstrap + deploy)"

build-backend:
	cd backend && ./gradlew build

test-backend:
	cd backend && ./gradlew test

test-backend-integration:
	cd backend && ./gradlew integrationTest

run-backend:
	cd backend && ./gradlew bootRun

clean-backend:
	cd backend && ./gradlew clean

docker-up:
	docker compose up -d

docker-down:
	docker compose down

infra-init:
	TF_CLI_CONFIG_FILE=$(CURDIR)/infra/terraform/terraformrc terraform -chdir=$(TERRAFORM_DIR) init

infra-plan: infra-init
	TF_CLI_CONFIG_FILE=$(CURDIR)/infra/terraform/terraformrc terraform -chdir=$(TERRAFORM_DIR) plan

infra-apply: infra-init
	TF_CLI_CONFIG_FILE=$(CURDIR)/infra/terraform/terraformrc terraform -chdir=$(TERRAFORM_DIR) apply

render-inventory:
	TF_DIR=$(TERRAFORM_DIR) VM_NAME=$${VM_NAME:-debtsapp-k3s} python scripts/resolve_ssh_host.py \
	  | ./scripts/render-ansible-inventory.sh - > $(K3S_INVENTORY)

deploy:
	ANSIBLE_CONFIG=$(ANSIBLE_DIR)/ansible.cfg ansible-playbook $(ANSIBLE_DIR)/site.yml -i $(K3S_INVENTORY)
