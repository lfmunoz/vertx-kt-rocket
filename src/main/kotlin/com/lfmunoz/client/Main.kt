package com.lfmunoz.client

import io.vertx.config.ConfigRetriever
import io.vertx.core.Launcher
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.kotlin.core.DeploymentOptions
import io.vertx.kotlin.core.http.HttpServerOptions
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.micrometer.MicrometerMetricsOptions
import io.vertx.kotlin.micrometer.VertxPrometheusOptions
import io.vertx.micrometer.MetricsDomain
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking



fun main(args: Array<String>) {


        runBlocking {


            System.setProperty(
                    LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
                    SLF4JLogDelegateFactory::class.java.name
            )

            // Prometheus Settings
            val prometheusPort = 9124
            val prometheusRoute = "/metrics"

            // Start Vertx
            val vertx = Vertx.vertx(returnVertxOptions(prometheusPort, prometheusRoute))
            // Read JSON Config
            val jsonConfig = awaitResult<JsonObject> { ConfigRetriever.create(vertx).getConfig(it) }

            // Client specific config
            val port = jsonConfig.getInteger("port", 8123)
            val host = jsonConfig.getString("host", "0.0.0.0")
            val sendDelay = jsonConfig.getLong("delayBetweenSend", 10000L)
            // Global Config
            val numOfClients = jsonConfig.getInteger("numOfClients", 10)
            val launchDelay = jsonConfig.getLong("launchDelay", 10L)

            val localAddresses = jsonConfig.getString("localAddress", "")
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
            println("Prometheus running on http://localhost:${prometheusPort}/${prometheusRoute}")
            println("-------------------------------------------")

            // Deploy Client Verticles
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
            }

        }
}

fun returnVertxOptions(port: Int, route: String): VertxOptions {
    val options = VertxOptions()
    options.metricsOptions = MicrometerMetricsOptions()
            .setEnabled(true)
            .addDisabledMetricsCategory(MetricsDomain.EVENT_BUS)
            .addDisabledMetricsCategory(MetricsDomain.NET_CLIENT)
            .addDisabledMetricsCategory(MetricsDomain.NET_SERVER)
            .addDisabledMetricsCategory(MetricsDomain.HTTP_SERVER)
            .addDisabledMetricsCategory(MetricsDomain.HTTP_CLIENT)
            .setPrometheusOptions(
                    VertxPrometheusOptions()
                            .setEnabled(true)
                            .setStartEmbeddedServer(true)
                            .setEmbeddedServerOptions(
                                    HttpServerOptions()
                                            .setPort(port)
                            )
                            .setEmbeddedServerEndpoint(route)
            )
    return options
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