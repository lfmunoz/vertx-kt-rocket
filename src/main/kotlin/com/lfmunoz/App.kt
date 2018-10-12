package com.lfmunoz

import com.lfmunoz.client.ClientVerticle
import com.lfmunoz.server.ServerVerticle
import io.micrometer.core.instrument.MeterRegistry
import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.kotlin.core.DeploymentOptions
import io.vertx.kotlin.core.http.HttpServerOptions
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.micrometer.MicrometerMetricsOptions
import io.vertx.kotlin.micrometer.VertxPrometheusOptions
import io.vertx.micrometer.MetricsDomain
import io.vertx.micrometer.backends.BackendRegistries
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking


fun main(args: Array<String>) = runBlocking {


    System.setProperty(
            io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
            SLF4JLogDelegateFactory::class.java.name
    )

    // Prometheus Settings
    val prometheusPort = 9123
    val prometheusRoute = "/metrics"

    // Net Server / Net Client Settings
    val port = 8123
    val host = "0.0.0.0"
    val delayBetweenSend = 500L
    val delayBetweenLaunch = 50L
    val numOfClients = 1000

    // Start Vertx
    val vertx = Vertx.vertx(returnVertxOptions(prometheusPort, prometheusRoute))
    // Read JSON Config
    val jsonConfig = awaitResult<JsonObject> { ConfigRetriever.create(vertx).getConfig(it) }

    // Create Configuration Data Object
   // val registry = BackendRegistries.getDefaultNow()
    val config = Config(
        port,
        host,
        delayBetweenSend
    //    registry
    )


    // Deploy Server Verticle
    val cores = Runtime.getRuntime().availableProcessors() / 2
    System.out.println("Deploying with cores = ${cores}")
    var options = DeploymentOptions( config = jsonConfig, instances = cores)
    awaitResult<String> { vertx.deployVerticle("com.lfmunoz.server.ServerVerticle", options, it) }

    // Deploy Client Verticles
    repeat(numOfClients) {
        delay(delayBetweenLaunch)
        launch {
            vertx.deployVerticle(ClientVerticle(it, config))
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
        val host: String,
        val delayBetweenSend: Long
       // val registry: MeterRegistry
)