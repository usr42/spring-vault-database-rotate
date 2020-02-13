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

@ConditionalOnBean(SecretLeaseContainer::class)
// tag::autowire[]
@Configuration
class VaultConfig(
        //tag::ignore_autowire[]
        private val applicationContext: ConfigurableApplicationContext,
        private val hikariDataSource: HikariDataSource,
        //end::ignore_autowire[]
        private val leaseContainer: SecretLeaseContainer,
        @Value("\${spring.cloud.vault.database.role}")
        private val databaseRole: String
) {
    // end::autowire[]

    // tag::detect_expiring[]
    @PostConstruct
    private fun postConstruct() {
        val vaultCredsPath = "database/creds/$databaseRole"
        leaseContainer.addLeaseListener { event ->
            if (event.path == vaultCredsPath) { // <1>
                log.info { "Lease change for DB: ($event) : (${event.lease})" }
                // tag::request_rotate[]
                if (event.isLeaseExpired && event.mode == RENEW) { // <1>
                    // tag::ignore1_request_rotate[]
                    // TODO Rotate the credentials here <2>
                    // end::ignore1_request_rotate[]
                    // tag::ignore_detect_expiring[]
                    log.info { "Replace RENEW for expired credential with ROTATE" }
                    leaseContainer.requestRotatingSecret(vaultCredsPath) // <2>
                    // tag::ignore2_request_rotate[]
                } else if (event is SecretLeaseCreatedEvent && event.mode == ROTATE) {
                    val credential = event.credential
                    updateDbProperties(credential)
                    updateDataSource(credential)
                    // end::ignore_detect_expiring[]
                    // end::ignore2_request_rotate[]
                }
                // end::request_rotate[]
            }
        }
    }
    // end::detect_expiring[]

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