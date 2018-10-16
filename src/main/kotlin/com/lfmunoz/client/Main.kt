package com.lfmunoz.client

import com.lfmunoz.SERVER_DEPLOY_SUMMARY
import io.micrometer.core.instrument.DistributionSummary
import io.vertx.config.ConfigRetriever
import io.vertx.core.Launcher
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.kotlin.core.DeploymentOptions
import io.vertx.kotlin.core.http.HttpServerOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.micrometer.MicrometerMetricsOptions
import io.vertx.kotlin.micrometer.VertxPrometheusOptions
import io.vertx.micrometer.MetricsDomain
import io.vertx.micrometer.backends.BackendRegistries
import kotlinx.coroutines.experimental.*
import java.util.concurrent.TimeUnit


class Main: CoroutineVerticle() {

    val registry = BackendRegistries.getDefaultNow()!!
    val deploySummary = DistributionSummary
            .builder(SERVER_DEPLOY_SUMMARY)
            .publishPercentiles(0.5, 0.95)
            .register(registry)

    override suspend fun start()  {
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

            println("-------------------------------------------")
            println("Launching ${numOfClients} clients")
            println("Binding to ${localAddresses}")
            println("Connecting to ${host} on ${port}")
            println("-------------------------------------------")

            // Deploy Client Verticles
            var start = System.nanoTime()
            repeat(numOfClients) {
                delay(launchDelay)
                launch {
                    val config = Config(
                            port,
                            host,
                            addressItr.next(),
                            sendDelay
                    )
                    vertx.deployVerticle(ClientVerticle(it, config))
                }
                val end = System.nanoTime()
                deploySummary.record(TimeUnit.NANOSECONDS.toMillis(end - start).toDouble())
                start = end
            }

        }
    }
}



data class Config(
        val port: Int,
        val remoteHost: String,
        val localHost: String,
        val sendDelay : Long
        // val registry: MeterRegistry
)


fun infiniteIterator(items :List<String>) = object : Iterator<String> {
    var idx = 0
    override fun hasNext(): Boolean {
        return true
    }
    override fun next(): String {
        return items[idx++ % items.size]
    }
}