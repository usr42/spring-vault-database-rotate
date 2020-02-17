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
// tag::complete[]
@Configuration
class VaultConfig(
        //tag::ignore_autowire[]
        private val applicationContext: ConfigurableApplicationContext,
        //tag::ignore_autowire2[]
        private val hikariDataSource: HikariDataSource,
        //end::ignore_autowire2[]
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
            if (event.path == vaultCredsPath) {
                log.info { "Lease change for DB: ($event) : (${event.lease})" }
                // tag::request_rotate[]
                // tag::get_rotated_secret[]
                if (event.isLeaseExpired && event.mode == RENEW) {
                    // tag::ignore_todo_rotate[]
                    // tag::ignore_complete[]
                    // TODO Rotate the credentials here <1>
                    // end::ignore_complete[]
                    // end::ignore_todo_rotate[]
                    // tag::ignore_detect_expiring[]
                    log.info { "Replace RENEW for expired credential with ROTATE" }
                    leaseContainer.requestRotatingSecret(vaultCredsPath) // <1>
                    // tag::ignore2_request_rotate[]
                } else if (event is SecretLeaseCreatedEvent && event.mode == ROTATE) { // <2>
                    val credential = event.credentials // <3>
                    // tag::ignore_complete[]
                    // TODO Update database connection
                    // end::ignore_complete[]
                    // tag::ignore_update_credentials[]
                    // tag::refresh_credentials[]
                    updateDbProperties(credential) // <1>
                    updateDataSource(credential) // <2>
                    // end::refresh_credentials[]
                    // end::ignore_update_credentials[]
                    // end::ignore_detect_expiring[]
                    // end::ignore2_request_rotate[]
                }
                // end::get_rotated_secret[]
                // end::request_rotate[]
            }
        }
    }
    // end::detect_expiring[]

    private val SecretLeaseEvent.path get() = source.path
    private val SecretLeaseEvent.isLeaseExpired
        get() = this is SecretLeaseExpiredEvent
    private val SecretLeaseEvent.mode get() = source.mode

    // tag::credentials_property_extension[]
    private val SecretLeaseCreatedEvent.credentials
        get() = Credential(get("username"), get("password")) // <1>

    private fun SecretLeaseCreatedEvent.get(param: String): String {
        val value = secrets[param] as? String // <2>
        if (value == null) { // <3>
            log.error {
                "Cannot update DB credentials (no $param available). Shutting down."
            }
            applicationContext.close()
            throw IllegalStateException("Cannot get $param from secrets")
        }
        return value // <4>
    }

    // tag::ignore_credentials_property_extension[]
    // tag::refresh_credentials_methods[]
    private fun updateDbProperties(credential: Credential) { // <1>
        val (username, password) = credential
        System.setProperty("spring.datasource.username", username)
        System.setProperty("spring.datasource.password", password)
    }

    private fun updateDataSource(credential: Credential) {
        val (username, password) = credential
        log.info { "==> Update database credentials" }
        hikariDataSource.hikariConfigMXBean.apply { // <2>
            setUsername(username)
            setPassword(password)
        }
        hikariDataSource.hikariPoolMXBean?.softEvictConnections() // <3>
                ?.also { log.info { "Soft Evict Hikari Data Source Connections" } }
                ?: log.warn { "CANNOT Soft Evict Hikari Data Source Connections" }
    }
    // end::refresh_credentials_methods[]

    companion object {
        private val log = KotlinLogging.logger { }
    }

    // end::ignore_credentials_property_extension[]
    private data class Credential(val username: String, val password: String)
    // end::credentials_property_extension[]
}
// end::complete[]