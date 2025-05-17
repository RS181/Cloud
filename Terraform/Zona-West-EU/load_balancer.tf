resource "azurerm_public_ip" "replica" {
  name                = "PublicIPForLB"
  location            = azurerm_resource_group.replica.location
  resource_group_name = azurerm_resource_group.replica.name
  allocation_method   = "Static"
}

resource "azurerm_lb" "replica" {
  name                = "ReplicaLoadBalancer"
  location            = azurerm_resource_group.replica.location
  resource_group_name = azurerm_resource_group.replica.name

  frontend_ip_configuration {
    name                 = "PublicIPAddress"
    public_ip_address_id = azurerm_public_ip.replica.id
  }
}


resource "azurerm_lb_backend_address_pool" "replica" {
  loadbalancer_id = azurerm_lb.replica.id
  name            = "vms-EU"
}

resource "azurerm_lb_rule" "replica_https" {
  loadbalancer_id                = azurerm_lb.replica.id
  name                           = "LBRule-HTTPS"
  protocol                       = "Tcp"
  frontend_port                  = 443
  backend_port                   = 443
  frontend_ip_configuration_name = "PublicIPAddress"
  backend_address_pool_ids       = [azurerm_lb_backend_address_pool.replica.id]
}

resource "azurerm_network_security_group" "lb_replica" {
  name                = "load-balancer-nsg"
  location            = azurerm_resource_group.replica.location
  resource_group_name = azurerm_resource_group.replica.name

  security_rule {
    name                       = "AllowLoadBalancerHealthProbe"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_address_prefix      = "AzureLoadBalancer"
    destination_port_range     = "*"
    destination_address_prefix = "*"
    source_port_range          = "*"
  }

  security_rule {
    name                   = "Allow-Cloudflare-HTTPS-IPv4"
    priority               = 110
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
    priority               = 120
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