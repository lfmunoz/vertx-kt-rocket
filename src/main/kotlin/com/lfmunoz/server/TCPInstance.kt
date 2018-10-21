package com.lfmunoz.server

////////////////////////////////////////////////////////////////////////////////
// import
////////////////////////////////////////////////////////////////////////////////
import com.lfmunoz.SERVER_ID_SERVICE
import com.lfmunoz.SERVER_PING_SUMMARY
import com.lfmunoz.SERVER_PONG_SUMMARY
import com.lfmunoz.logger
import io.micrometer.core.instrument.DistributionSummary
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.micrometer.backends.BackendRegistries
import kotlinx.coroutines.experimental.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import io.vertx.core.Context

////////////////////////////////////////////////////////////////////////////////
// Server Handler
////////////////////////////////////////////////////////////////////////////////
class TCPInstance(
    val context: Context,
    val socket: NetSocket,
    val pingDelay: Long
): CoroutineScope, Handler<Buffer> {
    val log by logger()

    override val coroutineContext: CoroutineContext
        get() = context.dispatcher() //Job()  //vertx.dispatcher()

    var id : Int = 0

    // Metric Fields
    val registry = BackendRegistries.getDefaultNow()!!
    val pingSummary = DistributionSummary
            .builder(SERVER_PING_SUMMARY)
            .publishPercentiles(0.5, 0.95)
            .register(registry)
    val pongSummary = DistributionSummary
            .builder(SERVER_PONG_SUMMARY)
            .publishPercentiles(0.5, 0.95)
            .register(registry)

    // Fields
    var sendIdx: Long = 0L
    var lastServerActivity: Long = System.nanoTime()
    var lastPingSent : Long = System.nanoTime()

    // Constructor
    init {
        socket.handler(this)
        socket.closeHandler {socketCloseHandler() }
        socket.exceptionHandler { socketExceptionHandler(it) }
        launch {
            id = getId()
            log.debug("ServerHandler ${id}")
            sendPings()
        }
    }

    override fun handle(buffer: Buffer) {
        val now = System.nanoTime()
        val deltaInMs = TimeUnit.NANOSECONDS.toMillis(now - lastServerActivity)
        lastServerActivity = now
        pongSummary.record(deltaInMs.toDouble())
        // log.trace("[$id] - ${buffer.getLong(0)} on ${Vertx.currentContext()}")

    }

    fun sendPings() {
        launch {
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


    fun socketExceptionHandler(err: Throwable ) {
       // exceptionCount.increment()
        log.error("socket exception: {} ", err.message)
    }

    fun socketCloseHandler() {
       // disconnectionCount.increment()
    }
}





