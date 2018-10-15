package com.lfmunoz.server

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
    private val connectionCount = registry.gauge("connectionCount", AtomicLong(0L))!!
    private val exceptionCount = registry.counter("exceptionCount")

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

        }

        server.close  {
            connectionCount.decrementAndGet()
        }

        server.exceptionHandler { err ->
            exceptionCount.increment()
            log.error(err.message)
        }

        try {
            awaitResult<NetServer> { server.listen(it) }
        } catch(e: Exception) {
            log.error("error with listener, ${e.message}")
        }
    }

}

fun infiniteIterator(items :List<String>) = object : Iterator<String> {
    var idx = 0
    override fun hasNext(): Boolean {
        return true
    }
    override fun next(): String {
        return items[idx++ % items.size]
    }
}