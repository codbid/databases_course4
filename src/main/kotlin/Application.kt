package com.example

import com.example.config.DatabaseFactory
import com.example.config.configureRouting
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSecurity()
    configureSerialization()
    configureRouting()

    install(ContentNegotiation) { jackson() }
    DatabaseFactory.init(this,environment.config)
}
