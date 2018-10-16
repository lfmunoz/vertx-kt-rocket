package com.lfmunoz.server

import com.lfmunoz.SERVER_CONNECTION_COUNT
import com.lfmunoz.SERVER_DISCONNECTION_COUNT
import com.lfmunoz.SERVER_EXCEPTION_COUNT
import com.lfmunoz.SERVER_ID_SERVICE
import io.micrometer.core.instrument.DistributionSummary
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.core.net.NetServerOptions
import io.vertx.kotlin.coroutines.*
import io.vertx.micrometer.backends.BackendRegistries
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.experimental.coroutineContext


////////////////////////////////////////////////////////////////////////////////
// Server Verticle
////////////////////////////////////////////////////////////////////////////////
class Verticle : CoroutineVerticle() {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.name) }

    private var contextId = "N/A"
    // fields
    private var port: Int = 8123
    private var delayBetweenSend = 15000L

    private val registry = BackendRegistries.getDefaultNow()!!

    private val connectionCount = registry.counter(SERVER_CONNECTION_COUNT)
    private val disconnectionCount = registry.counter(SERVER_DISCONNECTION_COUNT)
    private val exceptionCount = registry.counter(SERVER_EXCEPTION_COUNT)


    private val connMap: ConcurrentHashMap<Int, Handler> = ConcurrentHashMap<Int, Handler>()

    override suspend fun start() {
        contextId = context.deploymentID()


        port = config.getInteger("port", port)
        delayBetweenSend = config.getLong("delayBetweenSend", delayBetweenSend)

        log.info(
                "\n----------------------------------------------------------" +
                "\nServer - start() thread ${Vertx.currentContext()}" +
                "\n port = ${port}" +
                "\n delayBetweenSend = ${delayBetweenSend}" +
                "\n----------------------------------------------------------"
        )
        val instances = Runtime.getRuntime().availableProcessors() / 2
        startServer()
    }

    override suspend fun stop() {
        log.trace("Server - stop() thread ${Vertx.currentContext()} ")
    }

    private suspend fun startServer() {

            val options = NetServerOptions(port = port)
            val server = vertx.createNetServer(options)
            server.connectHandler {socket ->
                GlobalScope.launch(vertx.dispatcher()) {
                    val id = getId()
                    // Handle the connection in here
                    val serverHandler = Handler(vertx, id, socket, delayBetweenSend)
                    connMap.put(serverHandler.id, serverHandler)
                    socket.handler(serverHandler)

                    socket.closeHandler {
                        //  connectionCount.decrementAndGet()
                        disconnectionCount.increment()
                    }
                    socket.exceptionHandler { err ->
                        exceptionCount.increment()
                        log.error("socket exception: {} ", err.message)
                    }

                    connectionCount.increment()
                }
            }

            server.exceptionHandler { err ->
                log.error("server exception: {} ", err.message)
            }

            try {
                awaitResult<NetServer> { server.listen(it) }
            } catch (e: Exception) {
                log.error("error with listener, ${e.message}")
            }
    }


    suspend fun getId() : Int {
        // Send a message and wait for a reply
        try {
            val reply: Message<Int> = awaitResult<Message<Int>> { h ->
                vertx.eventBus().send(SERVER_ID_SERVICE, 0, h)
            }
            return reply.body()
        } catch(e: ReplyException) {
            // Handle specific reply exception here
            log.info("Reply failure: ${e.message}")
            return 0
        }
    }

}
