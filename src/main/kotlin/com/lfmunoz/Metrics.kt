package com.lfmunoz

import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.micrometer.backends.BackendRegistries


class Metrics: CoroutineVerticle() {

    private val registry = BackendRegistries.getDefaultNow()!!

    private val connectionCount = registry.counter(SERVER_CONNECTION_COUNT)
    private val disconnectionCount = registry.counter(SERVER_DISCONNECTION_COUNT)
    private val exceptionCount = registry.counter(SERVER_EXCEPTION_COUNT)


    ////////////////////////////////////////////////////////////////////////////////
    // Vertx methods
    ////////////////////////////////////////////////////////////////////////////////
    override suspend fun start() {
        vertx.eventBus().localConsumer<Int>("ConnectionCount").handler {
            connectionCount.increment()
        }

        vertx.eventBus().localConsumer<Int>("DisconnectionCount").handler {
            disconnectionCount.increment()
        }

        vertx.eventBus().localConsumer<Int>("ExceptionCount").handler {
            exceptionCount.increment()
        }

    }


}



fun connectionCountInc() : Int  {
    try {
        Vertx.vertx().eventBus().send(SERVER_ID_SERVICE, 0)
    } catch (e: ReplyException) {
        return 0
    }
}

