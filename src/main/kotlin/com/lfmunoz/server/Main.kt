package com.lfmunoz.server


import io.vertx.kotlin.core.DeploymentOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch

// Server
class Main : CoroutineVerticle() {
    override suspend fun start() {
        GlobalScope.launch(context.dispatcher()) {
            // Deploy Server Verticle
            var cores = Runtime.getRuntime().availableProcessors()
            System.out.println("Deploying with cores = ${cores}")
            var options = DeploymentOptions(config = config, instances = cores)
            awaitResult<String> { vertx.deployVerticle("com.lfmunoz.server.Verticle", options, it) }
        }
    }
}

