package com.example

import com.example.app.kafka.consumer.AnalyticsConsumer
import com.example.app.kafka.consumer.InventoryConsumer
import com.example.app.kafka.streams.BookStatsStream
import com.example.config.DatabaseFactory
import com.example.config.KafkaFactory
import com.example.config.Neo4jFactory
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
    install(ContentNegotiation) { jackson() }
    KafkaFactory.init(this)
    configureRouting()
    DatabaseFactory.init(this, environment.config)
    Neo4jFactory.init(environment.config, this)

    Thread { InventoryConsumer.start() }.start()
    Thread { AnalyticsConsumer.start() }.start()
    Thread { BookStatsStream.start() }.start()
}
