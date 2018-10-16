package com.lfmunoz.client

import com.lfmunoz.client.Config
import io.vertx.core.Vertx
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.core.net.NetClientOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import org.slf4j.LoggerFactory

////////////////////////////////////////////////////////////////////////////////
// ClientVerticle
////////////////////////////////////////////////////////////////////////////////
class ClientVerticle(val id: Int, val myConfig: Config): CoroutineVerticle() {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.name) }

    var socket: NetSocket? = null

    ////////////////////////////////////////////////////////////////////////////////
    // Vertx methods
    ////////////////////////////////////////////////////////////////////////////////
    override suspend fun start() {
        log.trace("ClientVerticle.start() - ${id} - ${Vertx.currentContext()}")
        connect()
    }

    override suspend fun stop() {
        log.trace("ClientVerticle.stop() - ${id} - ${Vertx.currentContext()}")
    }


    ////////////////////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////////////////////
    suspend fun connect() {
        val options = NetClientOptions(
                connectTimeout = 10000, localAddress = myConfig.localHost)
        val client = vertx.createNetClient(options)

        try {
            socket = awaitResult<NetSocket> { client.connect(myConfig.port, myConfig.remoteHost, it) }
            ClientHandler(vertx, id, socket, 10000L)

        } catch(e: Exception) {
            socket = null
            log.error("Error with connect, ${e.message}")
        }
    }

}


