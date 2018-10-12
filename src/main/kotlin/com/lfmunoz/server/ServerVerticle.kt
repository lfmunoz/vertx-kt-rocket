package com.lfmunoz.server

import com.lfmunoz.Config
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetServer
import io.vertx.kotlin.core.net.NetServerOptions
import io.vertx.kotlin.coroutines.*
import org.slf4j.LoggerFactory


class ServerVerticle : CoroutineVerticle() {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.simpleName) }

    var port: Int = 8123
    var host: String = "0.0.0.0"

    val connMap: HashMap<Int, ServerHandler> = HashMap<Int, ServerHandler>()
    var handlerId = 0

    override suspend fun start() {
        log.trace("Server - start() thread ${Vertx.currentContext()} ")
        port = config.getInteger("port", port)
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
            log.info("connectHandler() thread")

            val serverHandler = ServerHandler(vertx, handlerId++, socket)
            connMap.put(serverHandler.id, serverHandler)
            socket.handler(serverHandler)
            var buff = Buffer.buffer().appendLong(0L)
            socket.write(buff)
        }

        try {
            awaitResult<NetServer> { server.listen(it) }
        } catch(e: Exception) {
            log.error("error with listener, ${e.message}")
        }
    }


}
