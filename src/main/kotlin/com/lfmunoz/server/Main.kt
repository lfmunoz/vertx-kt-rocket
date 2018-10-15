package com.lfmunoz.server

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
import io.vertx.micrometer.VertxJmxMetricsOptions
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking


// server
fun main(args: Array<String>) {
    runBlocking {
        System.setProperty(
                LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
                SLF4JLogDelegateFactory::class.java.name
        )

        // Prometheus Settings
        val prometheusPort = 9123
        val prometheusRoute = "/metrics"

        // Start Vertx
        val vertx = Vertx.vertx(returnVertxOptions(prometheusPort, prometheusRoute))
        // Read JSON Config
        val jsonConfig = awaitResult<JsonObject> { ConfigRetriever.create(vertx).getConfig(it) }

        // Deploy Server Verticle
        val cores = Runtime.getRuntime().availableProcessors()
        System.out.println("Deploying with cores = ${cores}")
        var options = DeploymentOptions(config = jsonConfig, instances = cores)
        awaitResult<String> { vertx.deployVerticle("com.lfmunoz.server.Verticle", options, it) }
    }

}

class Main: Launcher() {




    override fun beforeStartingVertx(options: VertxOptions) {
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

