locals {
  data_inputs = {
    heading_one = var.heading_one
  }
}

resource "azurerm_resource_group" "replica" {
  name     = "Replicas-West-EU"
  location = "West Europe"
}

resource "azurerm_virtual_network" "replica" {
  name                = "replica-network"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.replica.location
  resource_group_name = azurerm_resource_group.replica.name
}

resource "azurerm_subnet" "replica" {
  name                 = "internal"
  resource_group_name  = azurerm_resource_group.replica.name
  virtual_network_name = azurerm_virtual_network.replica.name
  address_prefixes     = ["10.0.2.0/24"]
}

resource "azurerm_network_security_group" "replica" {
  name                = "replica-nsg"
  location            = azurerm_resource_group.replica.location
  resource_group_name = azurerm_resource_group.replica.name

  security_rule {
    name                       = "SSH"
    priority                   = 1000
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  # security_rule {
  #   name                       = "HTTP"
  #   priority                   = 1001
  #   direction                  = "Inbound"
  #   access                     = "Allow"
  #   protocol                   = "Tcp"
  #   source_port_range          = "*"
  #   destination_port_range     = "80"
  #   source_address_prefix      = "*"
  #   destination_address_prefix = "*"
  # }

  # security_rule {
  #   name                       = "HTTPS"
  #   priority                   = 1002
  #   direction                  = "Inbound"
  #   access                     = "Allow"
  #   protocol                   = "Tcp"
  #   source_port_range          = "*"
  #   destination_port_range     = "443"
  #   source_address_prefix      = "*"
  #   destination_address_prefix = "*"
  # }

  #Isto funciona (mas apenas permite trafego que venha da cloudfare)
  security_rule {
    name                   = "Allow-Cloudflare-HTTPS-IPv4"
    priority               = 1001
    direction              = "Inbound"
    access                 = "Allow"
    protocol               = "Tcp"
    source_port_range      = "*"
    destination_port_range = "443"
    source_address_prefixes = [
      "173.245.48.0/20",
      "103.21.244.0/22",
      "103.22.200.0/22",
      "103.31.4.0/22",
      "141.101.64.0/18",
      "108.162.192.0/18",
      "190.93.240.0/20",
      "188.114.96.0/20",
      "197.234.240.0/22",
      "198.41.128.0/17",
      "162.158.0.0/15",
      "104.16.0.0/13",
      "104.24.0.0/14",
      "172.64.0.0/13",
      "131.0.72.0/22",
    ]
    destination_address_prefix = "*"
  }

  security_rule {
    name                   = "Allow-Cloudflare-HTTPS-IPv6"
    priority               = 1002
    direction              = "Inbound"
    access                 = "Allow"
    protocol               = "Tcp"
    source_port_range      = "*"
    destination_port_range = "443"
    source_address_prefixes = [
      "2400:cb00::/32",
      "2606:4700::/32",
      "2803:f800::/32",
      "2405:b500::/32",
      "2405:8100::/32",
      "2a06:98c0::/29",
      "2c0f:f248::/32",
    ]
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "DenyAllInbound"
    priority                   = 2000
    direction                  = "Inbound"
    access                     = "Deny"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
}

resource "azurerm_subnet_network_security_group_association" "replica" {
  subnet_id                 = azurerm_subnet.replica.id
  network_security_group_id = azurerm_network_security_group.replica.id
}


resource "azurerm_linux_virtual_machine_scale_set" "replica" {
  name                = "replica-vmss-europe"
  resource_group_name = azurerm_resource_group.replica.name
  location            = azurerm_resource_group.replica.location
  sku                 = "Standard_F2"
  # autoscale é que vai gerir instâncias
  # instances           = 2
  admin_username = "adminuser"
  user_data      = base64encode(templatefile("userdata.tftpl", local.data_inputs))


  admin_ssh_key {
    username   = "adminuser"
    public_key = file("../Servidor_Origem/vm.pub")
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts"
    version   = "latest"
  }

  os_disk {
    storage_account_type = "Standard_LRS"
    caching              = "ReadWrite"
  }

  network_interface {
    name    = "replica"
    primary = true

    ip_configuration {
      name                                   = "internal"
      primary                                = true
      subnet_id                              = azurerm_subnet.replica.id
      load_balancer_backend_address_pool_ids = [azurerm_lb_backend_address_pool.replica.id]
    }
  }
}

resource "azurerm_monitor_autoscale_setting" "replica" {
  name                = "autoscale-vmss"
  resource_group_name = azurerm_resource_group.replica.name
  location            = azurerm_resource_group.replica.location
  target_resource_id  = azurerm_linux_virtual_machine_scale_set.replica.id

  profile {
    name = "defaultProfile"
    capacity {
      minimum = "2"
      maximum = "4"
      default = "2"
    }

    rule {
      metric_trigger {
        metric_name        = "Percentage CPU"
        metric_resource_id = azurerm_linux_virtual_machine_scale_set.replica.id
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT5M"
        time_aggregation   = "Average"
        operator           = "GreaterThan"
        threshold          = 75
      }

      scale_action {
        direction = "Increase"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT5M"
      }
    }

    rule {
      metric_trigger {
        metric_name        = "Percentage CPU"
        metric_resource_id = azurerm_linux_virtual_machine_scale_set.replica.id
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT5M"
        time_aggregation   = "Average"
        operator           = "LessThan"
        threshold          = 25
      }

      scale_action {
        direction = "Decrease"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT5M"
      }
    }
  }

  notification {
    email {
      send_to_subscription_administrator    = false
      send_to_subscription_co_administrator = false
      custom_emails                         = ["up202109728@up.pt"] # opcional
    }
  }

  tags = {
    environment = "Production"
  }
}
