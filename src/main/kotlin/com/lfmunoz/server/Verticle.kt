package com.lfmunoz.server

import com.lfmunoz.*
import io.micrometer.core.instrument.DistributionSummary
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.core.net.NetServerOptions
import io.vertx.kotlin.coroutines.*
import io.vertx.micrometer.backends.BackendRegistries
import kotlinx.coroutines.experimental.*
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.experimental.coroutineContext


////////////////////////////////////////////////////////////////////////////////
// Server Verticle
////////////////////////////////////////////////////////////////////////////////
class Verticle : CoroutineVerticle() {
    val log by logger()
    // fields
    private var contextId = "N/A"
    private var port: Int = 8123
    private var delayBetweenSend = 15000L

    private val connMap: ConcurrentHashMap<Int, TCPInstance> = ConcurrentHashMap()

    override suspend fun start() {
        config()
        startServer()
    }

    override suspend fun stop() {
        log.trace("Server - stop() thread ${Vertx.currentContext()} ")
    }

    private fun config() {
        contextId = context.deploymentID()
        port = config.getInteger("port", port)
        delayBetweenSend = config.getLong("delayBetweenSend", delayBetweenSend)
        log.info("""
            Started Server Verticle Instance:
             Context = ${Vertx.currentContext()}
             port = ${port}
             delayBetweenSend = ${delayBetweenSend}
        """.trimIndent())
    }


    private suspend fun startServer() {
        val options = NetServerOptions(port = port)
        val server = vertx.createNetServer(options)
        server.connectHandler {serverConnectionHandler(it)}
        server.exceptionHandler { serverExceptionHandler(it)}
        try {
            awaitResult<NetServer> { server.listen(it) }
        } catch (e: Exception) {
            log.error("Error with listener, ${e.message}")
        }
    }

    fun serverConnectionHandler(socket: NetSocket) {
        // Handle the connection in here
        val serverHandler = TCPInstance(vertx.orCreateContext,  socket, delayBetweenSend)
        connMap[serverHandler.id] = serverHandler
        socket.handler(serverHandler)

        //connectionCount.increment()
    }


    fun serverExceptionHandler(err: Throwable ) {
        // exceptionCount.increment()
        log.error("server exception: {} ", err.message)
    }

}
