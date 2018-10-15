package com.lfmunoz

import io.vertx.core.logging.SLF4JLogDelegateFactory
import kotlinx.coroutines.experimental.runBlocking


fun main(args: Array<String>) = runBlocking {


    System.setProperty(
            io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
            SLF4JLogDelegateFactory::class.java.name
    )
    println("Please run either the client or the server main method")

}
