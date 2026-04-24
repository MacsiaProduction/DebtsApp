variable "cloud_id" {
  description = "Yandex Cloud ID. Leave null to use YC_CLOUD_ID from the environment."
  type        = string
  default     = null
  nullable    = true
}

variable "folder_id" {
  description = "Yandex Cloud folder ID. Leave null to use YC_FOLDER_ID from the environment."
  type        = string
  default     = null
  nullable    = true
}

variable "zone" {
  description = "Yandex Cloud availability zone."
  type        = string
  default     = "ru-central1-a"
}

variable "vm_name" {
  description = "VM name prefix."
  type        = string
  default     = "debtsapp-k3s"
}

variable "network_name" {
  description = "Existing Yandex VPC network name to reuse."
  type        = string
  default     = "network"
}

variable "subnet_name" {
  description = "Existing Yandex VPC subnet name to reuse. Defaults to network-<zone>."
  type        = string
  default     = null
  nullable    = true
}

variable "ssh_user" {
  description = "Linux user used for SSH access."
  type        = string
  default     = "macsia"
}

variable "ssh_public_key" {
  description = "Public SSH key that will be installed on the VM."
  type        = string
}

variable "admin_password_hash" {
  description = "SHA-512 password hash for the primary admin user."
  type        = string
  sensitive   = true
}

variable "image_family" {
  description = "Ubuntu image family."
  type        = string
  default     = "ubuntu-2404-lts"
}

variable "platform_id" {
  description = "Yandex Compute platform ID."
  type        = string
  default     = "standard-v3"
}

variable "vm_cores" {
  description = "Number of CPU cores."
  type        = number
  default     = 4
}

variable "vm_memory_gb" {
  description = "RAM size in GB."
  type        = number
  default     = 8
}

variable "vm_core_fraction" {
  description = "Guaranteed CPU fraction percentage."
  type        = number
  default     = 50
}

variable "boot_disk_size_gb" {
  description = "Boot disk size in GB."
  type        = number
  default     = 50
}

variable "boot_disk_type" {
  description = "Boot disk type."
  type        = string
  default     = "network-ssd"
}

variable "preemptible" {
  description = "Whether to use a preemptible VM."
  type        = bool
  default     = false
}

variable "app_domain" {
  description = "Public domain that will point to the app."
  type        = string
}

variable "acme_email" {
  description = "Email for Let's Encrypt registration."
  type        = string
}
