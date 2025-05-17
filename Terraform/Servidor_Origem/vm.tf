locals {
  data_inputs = {
    heading_one = var.heading_one
  }
}

resource "azurerm_resource_group" "origin-server" {
  name     = "ServidorOrigem"
  location = "East US"
}

resource "azurerm_virtual_network" "origin-server" {
  name                = "origin-server-network"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.origin-server.location
  resource_group_name = azurerm_resource_group.origin-server.name
}

resource "azurerm_subnet" "origin-server" {
  name                 = "internal"
  resource_group_name  = azurerm_resource_group.origin-server.name
  virtual_network_name = azurerm_virtual_network.origin-server.name
  address_prefixes     = ["10.0.2.0/24"]
}

resource "azurerm_network_security_group" "origin-server" {
  name                = "origin-server-nsg"
  location            = azurerm_resource_group.origin-server.location
  resource_group_name = azurerm_resource_group.origin-server.name


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

  # Isto funciona (mas apenas permite trafego que venha da cloudfare)
  # security_rule {
  #   name                       = "Allow-Cloudflare-HTTPS-IPv4"
  #   priority                   = 1001
  #   direction                  = "Inbound"
  #   access                     = "Allow"
  #   protocol                   = "Tcp"
  #   source_port_range          = "*"
  #   destination_port_range     = "443"
  #   source_address_prefixes    = [
  #     "173.245.48.0/20",
  #     "103.21.244.0/22",
  #     "103.22.200.0/22",
  #     "103.31.4.0/22",
  #     "141.101.64.0/18",
  #     "108.162.192.0/18",
  #     "190.93.240.0/20",
  #     "188.114.96.0/20",
  #     "197.234.240.0/22",
  #     "198.41.128.0/17",
  #     "162.158.0.0/15",
  #     "104.16.0.0/13",
  #     "104.24.0.0/14",
  #     "172.64.0.0/13",
  #     "131.0.72.0/22",
  #   ]
  #   destination_address_prefix = "*"
  # }

  # security_rule {
  #   name                       = "Allow-Cloudflare-HTTPS-IPv6"
  #   priority                   = 1002
  #   direction                  = "Inbound"
  #   access                     = "Allow"
  #   protocol                   = "Tcp"
  #   source_port_range          = "*"
  #   destination_port_range     = "443"
  #   source_address_prefixes    = [
  #     "2400:cb00::/32",
  #     "2606:4700::/32",
  #     "2803:f800::/32",
  #     "2405:b500::/32",
  #     "2405:8100::/32",
  #     "2a06:98c0::/29",
  #     "2c0f:f248::/32",
  #   ]
  #   destination_address_prefix = "*"
  # }


  security_rule {
    name                       = "HTTP"
    priority                   = 1003
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "HTTPS"
    priority                   = 1004
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "AllowFlaskUploadAPI"
    priority                   = 1005
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "5000"
    source_address_prefix      = "*"
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


resource "azurerm_public_ip" "origin-server" {
  name                = "acceptanceTestPublicIp1"
  resource_group_name = azurerm_resource_group.origin-server.name
  location            = azurerm_resource_group.origin-server.location
  allocation_method   = "Static"

  tags = {
    environment = "Production"
  }
}

resource "azurerm_network_interface" "origin-server" {
  name                = "origin-server-nic"
  location            = azurerm_resource_group.origin-server.location
  resource_group_name = azurerm_resource_group.origin-server.name

  ip_configuration {
    name                          = "internal"
    subnet_id                     = azurerm_subnet.origin-server.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.origin-server.id
  }
}

resource "azurerm_network_interface_security_group_association" "origin-server" {
  network_interface_id      = azurerm_network_interface.origin-server.id
  network_security_group_id = azurerm_network_security_group.origin-server.id
}

resource "azurerm_linux_virtual_machine" "origin-server" {
  name                = "origin-server-machine"
  resource_group_name = azurerm_resource_group.origin-server.name
  location            = azurerm_resource_group.origin-server.location
  size                = "Standard_F2"
  admin_username      = "adminuser"
  user_data           = base64encode(templatefile("userdata.tftpl", local.data_inputs))
  network_interface_ids = [
    azurerm_network_interface.origin-server.id,
  ]

  admin_ssh_key {
    username   = "adminuser"
    public_key = file("vm.pub")
  }

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts"
    version   = "latest"
  }
}