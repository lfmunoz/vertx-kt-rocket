package com.lfmunoz

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
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    App().dispatch(args)
}

class App: Launcher() {

    var prometheusPort: Int = 9123
    var prometheusRoute: String = "/metrics"

    override fun afterConfigParsed(config: JsonObject) {
        prometheusPort = config.getInteger("prometheusPort", prometheusPort)
        prometheusRoute = config.getString("prometheusRoute", prometheusRoute)
    }

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
                                                .setPort(prometheusPort)
                                )
                                .setEmbeddedServerEndpoint(prometheusRoute)
                )
    }






}


