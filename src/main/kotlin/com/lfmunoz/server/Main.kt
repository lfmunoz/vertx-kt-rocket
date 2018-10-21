package com.lfmunoz.server

////////////////////////////////////////////////////////////////////////////////
// imports
////////////////////////////////////////////////////////////////////////////////
import com.lfmunoz.*
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.kotlin.core.DeploymentOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.micrometer.backends.BackendRegistries
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

////////////////////////////////////////////////////////////////////////////////
// ServerSuperVisor Verticle
////////////////////////////////////////////////////////////////////////////////
class Main : CoroutineVerticle() {
    val log by logger()

    private val registry = BackendRegistries.getDefaultNow()!!

    private val connectionCount = registry.counter(SERVER_CONNECTION_COUNT)
    private val disconnectionCount = registry.counter(SERVER_DISCONNECTION_COUNT)
    private val exceptionCount = registry.counter(SERVER_EXCEPTION_COUNT)


    ////////////////////////////////////////////////////////////////////////////////
    // Vertx methods
    ////////////////////////////////////////////////////////////////////////////////
    override suspend fun start() {

        vertx.deployVerticle(object : CoroutineVerticle() {
            val handlerId = AtomicInteger(0)

            override suspend fun start() {
                val consumer = vertx.eventBus().localConsumer<Int>(SERVER_ID_SERVICE)
                consumer.handler { message ->
                    message.reply(handlerId.incrementAndGet())
                }
            }
       // })
        }, DeploymentOptions().setWorker(true))

        val deploymentID = awaitResult<String> { vertx.deployVerticle("com.lfmunoz.server.Verticle", options(), it) }

    }
    ////////////////////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////////////////////
    fun options() : DeploymentOptions {
      val instances = Runtime.getRuntime().availableProcessors() / 2
      return DeploymentOptions(config = config, instances = instances)
    }
}



suspend fun getId() : Int  {
    try {
        return  awaitResult<Message<Int>> {
            Vertx.vertx().eventBus().send(SERVER_ID_SERVICE, 0, it)
        }.body()
    } catch (e: ReplyException) {
        return 0
    }
}