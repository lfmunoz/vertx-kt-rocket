package com.lfmunoz.server

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import org.slf4j.LoggerFactory

////////////////////////////////////////////////////////////////////////////////
// ServerHandler
////////////////////////////////////////////////////////////////////////////////
class ServerHandler(val vertx: Vertx, val id: Int, val socket: NetSocket) : Handler<Buffer> {

    private val log by lazy { LoggerFactory.getLogger(this.javaClass.simpleName) }

    var sendIdx: Long = 0L
    var receiveIdx: Long = 0L

    override fun handle(buffer: Buffer) {
        //log.info("[$id] - ${buffer.getLong(0)} on ${Vertx.currentContext()}")
        vertx.setTimer(500) {
            var buff = Buffer.buffer().appendLong(++sendIdx)
            socket.write(buff)
        }
    }
}