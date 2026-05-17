TERRAFORM_DIR := infra/terraform/yandex
ANSIBLE_DIR := infra/ansible
K3S_INVENTORY ?= $(ANSIBLE_DIR)/inventory.ini

.PHONY: help infra-init infra-plan infra-apply render-inventory deploy

help:
	@echo "infra-init        Initialize Terraform"
	@echo "infra-apply       Provision or update the VM"
	@echo "render-inventory  Render Ansible inventory from Terraform outputs"
	@echo "deploy            Run full site playbook (bootstrap + deploy)"

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
