package com.lfmunoz.server

import com.lfmunoz.SERVER_CONNECTION_COUNT
import com.lfmunoz.SERVER_EXCEPTION_COUNT
import io.micrometer.core.instrument.DistributionSummary
import io.vertx.core.Vertx
import io.vertx.core.net.NetServer
import io.vertx.kotlin.core.net.NetServerOptions
import io.vertx.kotlin.coroutines.*
import io.vertx.micrometer.backends.BackendRegistries
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


// server
class Verticle : CoroutineVerticle() {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.simpleName) }

    // fields
    private var port: Int = 8123
    private var delayBetweenSend = 15000L

    private val registry = BackendRegistries.getDefaultNow()!!
    private val connectionCount = registry.gauge(SERVER_CONNECTION_COUNT, AtomicLong(0L))!!
    private val exceptionCount = registry.counter(SERVER_EXCEPTION_COUNT)

    private val connMap: ConcurrentHashMap<Int, Handler> = ConcurrentHashMap<Int, Handler>()
    private var handlerId = 0

    override suspend fun start() {

        port = config.getInteger("port", port)
        delayBetweenSend = config.getLong("delayBetweenSend", delayBetweenSend)

        log.info(
                "\n----------------------------------------------------------" +
                "\nServer - start() thread ${Vertx.currentContext()}" +
                "\n port = ${port}" +
                "\n delayBetweenSend = ${delayBetweenSend}" +
                "\n----------------------------------------------------------"
        )
        startServer()
    }

    override suspend fun stop() {
        log.trace("Server - stop() thread ${Vertx.currentContext()} ")
    }

    private suspend fun startServer() {
        val options = NetServerOptions( port = port)
        val server = vertx.createNetServer(options)
        server.connectHandler{ socket ->
            // Handle the connection in here
            log.trace("connectHandler()")
            connectionCount.incrementAndGet()
            val serverHandler = Handler(vertx, handlerId++, socket, delayBetweenSend)
            connMap.put(serverHandler.id, serverHandler)
            socket.handler(serverHandler)

            socket.closeHandler {
                connectionCount.decrementAndGet()
            }
            socket.exceptionHandler { err ->
                exceptionCount.increment()
                log.error("socket exception: {} ", err.message)
            }


        }

        //server.close  {
        //}

        server.exceptionHandler { err ->
            log.error("server exception: {} ", err.message)
        }

        try {
            awaitResult<NetServer> { server.listen(it) }
        } catch(e: Exception) {
            log.error("error with listener, ${e.message}")
        }
    }

}
