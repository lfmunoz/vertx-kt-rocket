package com.lfmunoz.client

import com.lfmunoz.CLIENT_PING_SUMMARY
import com.lfmunoz.CLIENT_PONG_SUMMARY
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
// Handler
////////////////////////////////////////////////////////////////////////////////
class Handler(
    val vertx: Vertx,
    val id: Int,
    var socket: NetSocket?,
    val pingDelay: Long
) {
    private val log by lazy { LoggerFactory.getLogger(this.javaClass.name) }
    ////////////////////////////////////////////////////////////////////////////////
    // Fields
    ////////////////////////////////////////////////////////////////////////////////
    // Metrics
    val registry = BackendRegistries.getDefaultNow()!!
    val pingSummary = DistributionSummary
            .builder(CLIENT_PING_SUMMARY)
            .publishPercentiles(0.5, 0.95)
            .register(registry)
    val pongSummary = DistributionSummary
            .builder(CLIENT_PONG_SUMMARY)
            .publishPercentiles(0.5, 0.95)
            .register(registry)

    // Client State
    var sendIdx: Long = 0L
    var lastServerActivity: Long = System.nanoTime()
    var lastPingSent : Long = System.nanoTime()

    ////////////////////////////////////////////////////////////////////////////////
    // constructor
    ////////////////////////////////////////////////////////////////////////////////
    init {
        log.debug("Handler ${id}")
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
    // Methods
    ////////////////////////////////////////////////////////////////////////////////
    fun handle(buffer: Buffer) {
        //  log.trace("[$id] - ${buffer.getLong(0)} on ${Vertx.currentContext()}")
        val now = System.nanoTime()
        val deltaInMs = TimeUnit.NANOSECONDS.toMillis(now - lastServerActivity)
        lastServerActivity = now
        pongSummary.record(deltaInMs.toDouble())
    }
    fun sendPings() {
        GlobalScope.launch(vertx.dispatcher()) {
            while(true) {
                val now = System.nanoTime()
                socket?.write(Buffer.buffer().appendLong(++sendIdx))
                val deltaInMs = TimeUnit.NANOSECONDS.toMillis(now - lastPingSent)
                lastPingSent = now
                pingSummary.record(deltaInMs.toDouble())
                delay(pingDelay)
            }
        }
    }
}