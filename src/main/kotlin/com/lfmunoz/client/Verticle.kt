package com.lfmunoz.client

import com.lfmunoz.Config
import io.vertx.core.Vertx
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.core.net.NetClientOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import org.slf4j.LoggerFactory

////////////////////////////////////////////////////////////////////////////////
// Verticle
////////////////////////////////////////////////////////////////////////////////
class Verticle(val id: Int, val myConfig: Config): CoroutineVerticle() {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.name) }

    var socket: NetSocket? = null

    ////////////////////////////////////////////////////////////////////////////////
    // Vertx methods
    ////////////////////////////////////////////////////////////////////////////////
    override suspend fun start() {
        log.trace("Verticle.start() - ${id} - ${Vertx.currentContext()}")
        connect().await()
    }

    override suspend fun stop() {
        log.trace("Verticle.stop() - ${id} - ${Vertx.currentContext()}")
    }


    ////////////////////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////////////////////
    suspend fun connect() : Deferred<Boolean> {
        return GlobalScope.async(vertx.dispatcher()) {
            val options = NetClientOptions(
                    connectTimeout = 10000, localAddress = myConfig.localHost)
            val client = vertx.createNetClient(options)
            try {
                socket = awaitResult<NetSocket> { client.connect(myConfig.port, myConfig.remoteHost, it) }
                Handler(vertx, id, socket, 10000L)
                true
            } catch(e: Exception) {
                socket = null
                log.error("Error with connect, ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

}


