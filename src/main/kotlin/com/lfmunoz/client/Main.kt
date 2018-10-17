package com.lfmunoz.client
////////////////////////////////////////////////////////////////////////////////
// imports
////////////////////////////////////////////////////////////////////////////////
import com.lfmunoz.CLIENT_DEPLOY_SUMMARY
import com.lfmunoz.Config
import com.lfmunoz.infiniteIterator
import io.micrometer.core.instrument.DistributionSummary
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.micrometer.backends.BackendRegistries
import kotlinx.coroutines.experimental.*
import java.util.concurrent.TimeUnit

////////////////////////////////////////////////////////////////////////////////
// Client Verticle
//   Handles management and deployment of all the client Verticles
////////////////////////////////////////////////////////////////////////////////
class Main : CoroutineVerticle() {
    private val log by lazy { org.slf4j.LoggerFactory.getLogger(this.javaClass.name) }
    val registry = BackendRegistries.getDefaultNow()!!
    val deploySummary = DistributionSummary
            .builder(CLIENT_DEPLOY_SUMMARY)
            .publishPercentiles(0.5, 0.95)
            .register(registry)
    ////////////////////////////////////////////////////////////////////////////////
    // Verticle methods start()/stop()
    ////////////////////////////////////////////////////////////////////////////////
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
            log.info("Launch delay set to ${launchDelay}")
            log.info("-------------------------------------------")

            // Deploy Client Verticles

            log.info("Starting deployment of {} clients", numOfClients)
            var start = System.nanoTime()
            repeat(numOfClients) {
                val config = Config( port, host, addressItr.next(), sendDelay )
                val id = it
                try {
                    awaitResult<String> { vertx.deployVerticle(Verticle(id, config), it) }
                    //awaitResult<String> { handler -> vertx.deployVerticle(Verticle(id, config), handler) }
                } catch (e: Exception) {
                    log.error("Deploy error - {}", e.message)
                }
                val end = System.nanoTime()
                deploySummary.record(TimeUnit.NANOSECONDS.toMillis(end - start).toDouble())
                start = end
                delay(launchDelay)
            }
            log.info("Finished deploying {} clients", numOfClients)
        }
    }
} // end of class


