package com.lfmunoz.client

import com.lfmunoz.client.Config
import io.vertx.core.Vertx
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.core.net.NetClientOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import org.slf4j.LoggerFactory

class ClientVerticle(val id: Int, val myConfig: Config): CoroutineVerticle() {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.simpleName) }

    override suspend fun start() {

        log.trace("${Thread.currentThread()} Client - start() thread ")
        log.trace("${Vertx.currentContext()} Client - start() vertx context")

        val options = NetClientOptions(
                connectTimeout = 10000, localAddress = myConfig.localHost)
        val client = vertx.createNetClient(options)

        try {
            val socket = awaitResult<NetSocket> { client.connect(myConfig.port, myConfig.remoteHost, it) }
            ClientHandler(vertx, id, socket, 1000L)
        } catch(e: Exception) {
            log.error("error with connect, ${e.message}")
        }



    }

    override suspend fun stop() {
        log.trace("${Thread.currentThread()} Client - stop() thread ")
        log.trace("${Vertx.currentContext()} Client - stop() vertx context")

    }


}


