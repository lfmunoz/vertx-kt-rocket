package com.lfmunoz.server

import com.lfmunoz.SERVER_PING_SUMMARY
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
// Server Handler
////////////////////////////////////////////////////////////////////////////////
class Handler(
        val vertx: Vertx,
        val id: Int,
        val socket: NetSocket?,
        val pingDelay: Long
) : Handler<Buffer> {
    private val log by lazy { LoggerFactory.getLogger(this.javaClass.name) }

    ////////////////////////////////////////////////////////////////////////////////
    // fields
    ////////////////////////////////////////////////////////////////////////////////
    var sendIdx: Long = 0L
    var lastServerActivity: Long = System.nanoTime()

    val registry = BackendRegistries.getDefaultNow()!!
    val pingSummary = DistributionSummary
            .builder(SERVER_PING_SUMMARY)
            .publishPercentiles(0.5, 0.95)
            .register(registry)

    ////////////////////////////////////////////////////////////////////////////////
    // constructor
    ////////////////////////////////////////////////////////////////////////////////
    init {
        log.debug("ServerHandler ${id}")
        sendPings()
    }
    ////////////////////////////////////////////////////////////////////////////////
    // methods
    ////////////////////////////////////////////////////////////////////////////////
    override fun handle(buffer: Buffer) {
        val now = System.nanoTime()
        val deltaInMs = TimeUnit.NANOSECONDS.toMillis(now - lastServerActivity)
        lastServerActivity = now
        pingSummary.record(deltaInMs.toDouble())
       // log.trace("[$id] - ${buffer.getLong(0)} on ${Vertx.currentContext()}")

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
} // end of class