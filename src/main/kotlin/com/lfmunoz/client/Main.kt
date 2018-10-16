package com.lfmunoz.client

import com.lfmunoz.CLIENT_DEPLOY_SUMMARY
import io.micrometer.core.instrument.DistributionSummary
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.micrometer.backends.BackendRegistries
import kotlinx.coroutines.experimental.*
import java.util.concurrent.TimeUnit


class Main : CoroutineVerticle() {
    private val log by lazy { org.slf4j.LoggerFactory.getLogger(this.javaClass.name) }
    val registry = BackendRegistries.getDefaultNow()!!
    val deploySummary = DistributionSummary
            .builder(CLIENT_DEPLOY_SUMMARY)
            .publishPercentiles(0.5, 0.95)
            .register(registry)

    override suspend fun start() {
        println(this.javaClass.name)
        GlobalScope.launch(context.dispatcher()) {

            // Client specific config
            val port = config.getInteger("port", 8123)
            val host = config.getString("host", "0.0.0.0")
            val sendDelay = config.getLong("delayBetweenSend", 10000L)
            // Global Config
            val numOfClients = config.getInteger("numOfClients", 10)
            val launchDelay = config.getLong("launchDelay", 10L)

            val localAddresses = config.getString("localAddress", "")
                    .split(",")
                    .mapNotNull {
                        val address = it.trim()
                        if (address.isBlank()) "0.0.0.0" else address
                    }
            val addressItr = infiniteIterator(localAddresses)

            log.info("-------------------------------------------")
            log.info("Launching ${numOfClients} clients")
            log.info("Binding to ${localAddresses}")
            log.info("Connecting to ${host} on ${port}")
            log.info("-------------------------------------------")

            // Deploy Client Verticles

            log.info("Starting deployment of {} clients", numOfClients)
            var start = System.nanoTime()
            repeat(numOfClients) {
                delay(launchDelay)
                val config = Config(
                        port,
                        host,
                        addressItr.next(),
                        sendDelay
                )
                val id = it
                try {
                    awaitResult<String> { vertx.deployVerticle(ClientVerticle(id, config), it) }
                } catch (e: Exception) {
                    log.error("Deploy error - {}", e.message)
                }
                val end = System.nanoTime()
                deploySummary.record(TimeUnit.NANOSECONDS.toMillis(end - start).toDouble())
                start = end
            }
            log.info("Finished deploying {} clients", numOfClients)


        }
    }
}


data class Config(
        val port: Int,
        val remoteHost: String,
        val localHost: String,
        val sendDelay: Long
        // val registry: MeterRegistry
)


fun infiniteIterator(items: List<String>) = object : Iterator<String> {
    var idx = 0
    override fun hasNext(): Boolean {
        return true
    }

    override fun next(): String {
        return items[idx++ % items.size]
    }
}