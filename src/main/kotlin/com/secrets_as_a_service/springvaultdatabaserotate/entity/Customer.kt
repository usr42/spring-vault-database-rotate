package com.secrets_as_a_service.springvaultdatabaserotate.entity

import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Customer(@Id val id: Int, val name: String, val address: String)