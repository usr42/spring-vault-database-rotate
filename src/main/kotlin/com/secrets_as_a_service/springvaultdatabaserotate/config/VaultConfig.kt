package com.secrets_as_a_service.springvaultdatabaserotate.config

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.vault.core.lease.SecretLeaseContainer
import org.springframework.vault.core.lease.domain.RequestedSecret.Mode.RENEW
import org.springframework.vault.core.lease.domain.RequestedSecret.Mode.ROTATE
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent
import org.springframework.vault.core.lease.event.SecretLeaseEvent
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent
import javax.annotation.PostConstruct

@Configuration
@ConditionalOnBean(SecretLeaseContainer::class)
class VaultConfig(
        private val leaseContainer: SecretLeaseContainer,
        @Value("\${spring.cloud.vault.database.role}")
        private val databaseRole: String,
        private val applicationContext: ConfigurableApplicationContext,
        private val hikariDataSource: HikariDataSource
) {

    @PostConstruct
    private fun postConstruct() {
        val vaultCredsPath = "database/creds/$databaseRole"
        leaseContainer.addLeaseListener { event ->
            if (event.path == vaultCredsPath) {
                log.info { "Lease change for DB: ($event) : (${event.lease})" }
                if (event.isLeaseExpired && event.mode == RENEW) {
                    log.info { "Replace RENEW for expired credential with ROTATE" }
                    leaseContainer.requestRotatingSecret(vaultCredsPath)
                } else if (event is SecretLeaseCreatedEvent && event.mode == ROTATE) {
                    val credential = event.credential
                    updateDbProperties(credential)
                    updateDataSource(credential)
                }
            }
        }
    }

    private val SecretLeaseEvent.path get() = source.path
    private val SecretLeaseEvent.isLeaseExpired
        get() = this is SecretLeaseExpiredEvent
    private val SecretLeaseEvent.mode get() = source.mode

    private val SecretLeaseCreatedEvent.credential
        get() = Credential(get("username"), get("password"))

    private fun SecretLeaseCreatedEvent.get(param: String): String {
        val value = secrets[param] as? String
        if (value == null) {
            log.error {
                "Cannot update DB credentials (no $param available). Shutting down."
            }
            applicationContext.close()
            throw IllegalStateException("Cannot get $param from secrets")
        }
        return value
    }

    private fun updateDbProperties(credential: Credential) {
        val (username, password) = credential
        System.setProperty("spring.datasource.username", username)
        System.setProperty("spring.datasource.password", password)
    }

    private fun updateDataSource(credential: Credential) {
        val (username, password) = credential
        log.info { "==> Update database credentials" }
        hikariDataSource.hikariConfigMXBean.apply {
            setUsername(username)
            setPassword(password)
        }
        hikariDataSource.hikariPoolMXBean?.softEvictConnections()
                ?.also { log.info { "Soft Evict Hikari Data Source Connections" } }
                ?: log.info { "CANNOT Soft Evict Hikari Data Source Connections" }
    }

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private data class Credential(val username: String, val password: String)
}