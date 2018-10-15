package com.lfmunoz.client

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory

////////////////////////////////////////////////////////////////////////////////
// ClientHandler
////////////////////////////////////////////////////////////////////////////////
class ClientHandler(
    val vertx: Vertx,
    val id: Int,
    val socket: NetSocket,
    val pingDelay: Long
) : Handler<Buffer> {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.simpleName) }

    var sendIdx: Long = 0L
    var receiveIdx: Long = 0L

    var lastServerActivity: Long = System.nanoTime()

    override fun handle(buffer: Buffer) {
        lastServerActivity = System.nanoTime()
        log.trace("[$id] - ${buffer.getLong(0)} on ${Vertx.currentContext()}")
        vertx.setTimer(1000) {
            var buff = Buffer.buffer().appendLong(++sendIdx)
            socket.write(buff)
        }
    }
    fun sendPings() {
        GlobalScope.launch(vertx.dispatcher()) {
            while(true) {
                var buff = Buffer.buffer().appendLong(++sendIdx)
                socket.write(buff)
                delay(pingDelay)
            }
        }
    }
}