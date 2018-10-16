package com.lfmunoz

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

fun main(args: Array<String>) {
    println("Experimental Main")


    val vertx = Vertx.vertx()



    vertx.deployVerticle(object : CoroutineVerticle() {
        override suspend fun start() {
            test()
        }


        fun test() {
            GlobalScope.launch(vertx.dispatcher()) {
                while (true) {
                    delay(1000)
                   println("tick " + Thread.currentThread().name)

                }
            }
        }

    })
   // }, DeploymentOptions().setWorker(true))
}