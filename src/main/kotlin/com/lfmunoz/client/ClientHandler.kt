package com.lfmunoz.client

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory

////////////////////////////////////////////////////////////////////////////////
// ClientHandler
////////////////////////////////////////////////////////////////////////////////
class ClientHandler(val vertx: Vertx, val id: Int, val socket: NetSocket) : Handler<Buffer> {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.simpleName) }

    var sendIdx: Long = 0L
    var receiveIdx: Long = 0L

    var lastServerActivity: Long = System.nanoTime()

    override fun handle(buffer: Buffer) {
        lastServerActivity = System.nanoTime()
        log.trace("[$id] - ${buffer.getLong(0)} on ${Vertx.currentContext()}")
        vertx.setTimer(500) {
            var buff = Buffer.buffer().appendLong(++sendIdx)
            socket.write(buff)
        }
    }

}