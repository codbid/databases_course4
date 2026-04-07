package com.example

import com.example.app.kafka.consumer.AnalyticsConsumer
import com.example.app.kafka.consumer.InventoryConsumer
import com.example.app.kafka.streams.BookStatsStream
import com.example.app.pipeline.PipelineService
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
    PipelineService.init(this)
    if (KafkaFactory.isInitialized) {
        runCatching { PipelineService.ensureTopics() }
            .onFailure { log.warn("Kafka topics were not ensured during startup: ${it.message}") }
    }
    configureRouting()
    runCatching { DatabaseFactory.init(this, environment.config) }
        .onFailure { log.warn("Database initialization failed: ${it.message}") }
    runCatching { Neo4jFactory.init(environment.config, this) }
        .onFailure { log.warn("Neo4j initialization failed: ${it.message}") }

    if (KafkaFactory.isInitialized) {
        Thread { runCatching { InventoryConsumer.start() }.onFailure { log.warn("Inventory consumer stopped: ${it.message}") } }.start()
        Thread { runCatching { AnalyticsConsumer.start() }.onFailure { log.warn("Analytics consumer stopped: ${it.message}") } }.start()
        Thread { runCatching { BookStatsStream.start() }.onFailure { log.warn("Book stats stream stopped: ${it.message}") } }.start()
    }
}
