data "yandex_compute_image" "ubuntu" {
  family = var.image_family
}

locals {
  subnet_name = coalesce(var.subnet_name, "network-${var.zone}")
}

data "yandex_vpc_network" "debtsapp" {
  name = var.network_name
}

data "yandex_vpc_subnet" "debtsapp" {
  name = local.subnet_name
}

resource "yandex_compute_instance" "debtsapp" {
  name                      = var.vm_name
  platform_id               = var.platform_id
  zone                      = var.zone
  allow_stopping_for_update = true

  resources {
    cores         = var.vm_cores
    memory        = var.vm_memory_gb
    core_fraction = var.vm_core_fraction
  }

  boot_disk {
    initialize_params {
      image_id = data.yandex_compute_image.ubuntu.id
      size     = var.boot_disk_size_gb
      type     = var.boot_disk_type
    }
  }

  network_interface {
    subnet_id = data.yandex_vpc_subnet.debtsapp.id
    nat       = true
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
