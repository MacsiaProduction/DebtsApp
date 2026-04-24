output "public_ip" {
  description = "Public IPv4 address of the app VM."
  value       = yandex_compute_instance.debtsapp.network_interface[0].nat_ip_address
}

output "private_ip" {
  description = "Internal IPv4 address of the app VM."
  value       = yandex_compute_instance.debtsapp.network_interface[0].ip_address
}

output "ssh_user" {
  description = "SSH username for Ansible."
  value       = var.ssh_user
}

output "ssh_port" {
  description = "SSH port for Ansible."
  value       = 22
}

output "app_domain" {
  description = "Configured public domain."
  value       = var.app_domain
}

output "acme_email" {
  description = "Configured Let's Encrypt email."
  value       = var.acme_email
}
