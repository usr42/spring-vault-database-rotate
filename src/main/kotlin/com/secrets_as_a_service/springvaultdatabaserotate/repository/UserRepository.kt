package com.secrets_as_a_service.springvaultdatabaserotate.repository

import com.secrets_as_a_service.springvaultdatabaserotate.entity.Customer
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<Customer, Int>