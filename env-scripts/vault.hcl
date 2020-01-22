storage "file" {
  path = "./vault-storage"
}

listener "tcp" {
  address     = "127.0.0.1:8200"
  tls_disable = 1
}

max_lease_ttl = "3000h"