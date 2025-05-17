resource "azurerm_resource_group" "client-asia" {
  name     = "Client-East-Asia"
  location = "East Asia"
}

resource "azurerm_virtual_network" "client-asia" {
  name                = "client-asia-network"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.client-asia.location
  resource_group_name = azurerm_resource_group.client-asia.name
}

resource "azurerm_subnet" "client-asia" {
  name                 = "internal"
  resource_group_name  = azurerm_resource_group.client-asia.name
  virtual_network_name = azurerm_virtual_network.client-asia.name
  address_prefixes     = ["10.0.2.0/24"]
}

resource "azurerm_network_security_group" "client-asia" {
  name                = "client-asia-nsg"
  location            = azurerm_resource_group.client-asia.location
  resource_group_name = azurerm_resource_group.client-asia.name

  security_rule {
    name                       = "SSH"
    priority                   = 1001
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "HTTPS"
    priority                   = 1003
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

}


resource "azurerm_public_ip" "client-asia" {
  name                = "acceptanceTestPublicIp1"
  resource_group_name = azurerm_resource_group.client-asia.name
  location            = azurerm_resource_group.client-asia.location
  allocation_method   = "Static"

  tags = {
    environment = "Production"
  }
}

resource "azurerm_network_interface" "client-asia" {
  name                = "client-asia-nic"
  location            = azurerm_resource_group.client-asia.location
  resource_group_name = azurerm_resource_group.client-asia.name

  ip_configuration {
    name                          = "internal"
    subnet_id                     = azurerm_subnet.client-asia.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.client-asia.id
  }
}

resource "azurerm_network_interface_security_group_association" "client-asia" {
  network_interface_id      = azurerm_network_interface.client-asia.id
  network_security_group_id = azurerm_network_security_group.client-asia.id
}

resource "azurerm_linux_virtual_machine" "client-asia" {
  name                = "client-asia-machine"
  resource_group_name = azurerm_resource_group.client-asia.name
  location            = azurerm_resource_group.client-asia.location
  size                = "Standard_F2"
  admin_username      = "adminuser"
  network_interface_ids = [
    azurerm_network_interface.client-asia.id,
  ]

  admin_ssh_key {
    username   = "adminuser"
    public_key = file("../Servidor_Origem/vm.pub")
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