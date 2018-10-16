package com.lfmunoz.server

////////////////////////////////////////////////////////////////////////////////
// imports
////////////////////////////////////////////////////////////////////////////////
import com.lfmunoz.SERVER_ID_SERVICE
import io.vertx.core.DeploymentOptions
import io.vertx.kotlin.core.DeploymentOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

////////////////////////////////////////////////////////////////////////////////
// Server Deployer and Management
////////////////////////////////////////////////////////////////////////////////
class Main : CoroutineVerticle() {
    private val log by lazy { LoggerFactory.getLogger(this.javaClass.name) }

    private val handlerId = AtomicInteger(0)
    ////////////////////////////////////////////////////////////////////////////////
    // Vertx methods
    ////////////////////////////////////////////////////////////////////////////////
    override suspend fun start() {


        vertx.deployVerticle(object : CoroutineVerticle() {
            override suspend fun start() {
                val consumer = vertx.eventBus().localConsumer<Int>(SERVER_ID_SERVICE)
                consumer.handler { message ->
                    message.reply(handlerId.incrementAndGet())
                }
            }
       // })
        }, DeploymentOptions().setWorker(true))

        awaitResult<String> { vertx.deployVerticle("com.lfmunoz.server.Verticle", options(), it) }
    }
    ////////////////////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////////////////////
    fun options() : DeploymentOptions {
      val instances = Runtime.getRuntime().availableProcessors() / 2
      return DeploymentOptions(config = config, instances = instances)
      //  return DeploymentOptions(config = config)
    }


    //fun events() {

       // val eventBus = vertx.eventBus()!!
      //  val connectedChannel = eventBus.localConsumer<Int>(SERVER_CONNECTED_ADDR).toChannel(vertx)

        /*
        eventBus.localConsumer<Long>(AgentAddresses.CONNECTED.address) { serialNumberMsg ->
            log.debug("Received connected event from: {}", serialNumberMsg.body())
            connectedCount.incrementAndGet()
            registry.counter(CONNECT_COUNT).increment()
        }
        */
   // }
}

