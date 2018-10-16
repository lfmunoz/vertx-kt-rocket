package com.lfmunoz.client

import com.lfmunoz.CLIENT_PING_SUMMARY
import io.micrometer.core.instrument.DistributionSummary
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.micrometer.backends.BackendRegistries
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

////////////////////////////////////////////////////////////////////////////////
// ClientHandler
////////////////////////////////////////////////////////////////////////////////
class ClientHandler(
    val vertx: Vertx,
    val id: Int,
    var socket: NetSocket?,
    val pingDelay: Long
) {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.name) }

    // Metrics
    val registry = BackendRegistries.getDefaultNow()!!
    val pingSummary = DistributionSummary
            .builder(CLIENT_PING_SUMMARY)
            .publishPercentiles(0.5, 0.95)
            .register(registry)

    // Fields
    var sendIdx: Long = 0L
    var lastServerActivity: Long = System.nanoTime()

    // constructor
    init {
        log.debug("ClientHandler ${id}")
        // initialize socket handler
        socket?.handler{ handle(it) }
        socket?.closeHandler {
            this.socket = null
            log.debug("${id} socket closed")
        }
        socket?.exceptionHandler{
            this.socket = null
            log.error("${id} socket exception {}", it.message)
        }
        sendPings()
    }
    ////////////////////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////////////////////
    fun handle(buffer: Buffer) {
        //  log.trace("[$id] - ${buffer.getLong(0)} on ${Vertx.currentContext()}")
        val now = System.nanoTime()
        val deltaInMs = TimeUnit.NANOSECONDS.toMillis(now - lastServerActivity)
        lastServerActivity = now
        pingSummary.record(deltaInMs.toDouble())
    }
    fun sendPings() {
        GlobalScope.launch(vertx.dispatcher()) {
            while(true) {
                var buff = Buffer.buffer().appendLong(++sendIdx)
                socket?.write(buff)
                delay(pingDelay)
            }
        }
    }
}