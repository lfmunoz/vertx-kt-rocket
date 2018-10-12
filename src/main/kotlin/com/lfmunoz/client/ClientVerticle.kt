package com.lfmunoz.client

import com.lfmunoz.Config
import io.vertx.core.Vertx
import io.vertx.kotlin.core.net.NetClientOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory

class ClientVerticle(val id: Int, val myConfig: Config): CoroutineVerticle() {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.simpleName) }

    override suspend fun start() {

        log.trace("${Thread.currentThread()} Client - start() thread ")
        log.trace("${Vertx.currentContext()} Client - start() vertx context")

        var options = NetClientOptions(
                connectTimeout = 10000)
        var client = vertx.createNetClient(options)
        client.connect(myConfig.port, myConfig.host) { res ->
            if (res.succeeded()) {
           //     log.info("[${id}] - Connected!")
                var socket = res.result()

                val clientHandler = ClientHandler(vertx, id, socket)
                socket.handler(clientHandler)
            } else {
                log.error("Failed to connect: ${res.cause().message}")
            }
        }
    }

    override suspend fun stop() {
        log.trace("${Thread.currentThread()} Client - stop() thread ")
        log.trace("${Vertx.currentContext()} Client - stop() vertx context")

    }


}