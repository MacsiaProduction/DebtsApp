data "yandex_compute_image" "ubuntu" {
  family = var.image_family
}

resource "yandex_vpc_network" "debtsapp" {
  name = "${var.vm_name}-network"
}

resource "yandex_vpc_subnet" "debtsapp" {
  name           = "${var.vm_name}-subnet"
  zone           = var.zone
  network_id     = yandex_vpc_network.debtsapp.id
  v4_cidr_blocks = [var.subnet_cidr]
}

resource "yandex_vpc_security_group" "debtsapp" {
  name        = "${var.vm_name}-sg"
  description = "DebtsApp VM access"
  network_id  = yandex_vpc_network.debtsapp.id

  ingress {
    description    = "SSH"
    protocol       = "TCP"
    port           = 22
    v4_cidr_blocks = var.web_ingress_cidrs
  }

  ingress {
    description    = "HTTP"
    protocol       = "TCP"
    port           = 80
    v4_cidr_blocks = var.web_ingress_cidrs
  }

  ingress {
    description    = "HTTPS"
    protocol       = "TCP"
    port           = 443
    v4_cidr_blocks = var.web_ingress_cidrs
  }

  egress {
    description    = "Allow all outbound traffic"
    protocol       = "ANY"
    v4_cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "yandex_compute_instance" "debtsapp" {
  name                      = var.vm_name
  platform_id               = var.platform_id
  zone                      = var.zone
  allow_stopping_for_update = true

  resources {
    cores  = var.vm_cores
    memory = var.vm_memory_gb
  }

  boot_disk {
    initialize_params {
      image_id = data.yandex_compute_image.ubuntu.id
      size     = var.boot_disk_size_gb
      type     = var.boot_disk_type
    }
  }

  network_interface {
    subnet_id          = yandex_vpc_subnet.debtsapp.id
    nat                = true
    security_group_ids = [yandex_vpc_security_group.debtsapp.id]
  }

  scheduling_policy {
    preemptible = var.preemptible
  }

  metadata = {
    user-data = templatefile("${path.module}/cloud-init.yaml.tftpl", {
      vm_name             = var.vm_name
      ssh_user            = var.ssh_user
      ssh_public_key      = trimspace(var.ssh_public_key)
      admin_password_hash = var.admin_password_hash
    })
  }
}
