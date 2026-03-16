package com.example.app.kafka

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

data class KafkaMessageRequest(
    val key: String? = null,
    val value: String
)

fun Route.kafka(application: Application) {
    val producerService = KafkaProducerService(application)
    val consumerService = KafkaConsumerService(application).also { it.start() }

    post("/kafka/test") {
        val body = call.receive<KafkaMessageRequest>()
        producerService.sendMessage(body.key, body.value)
        call.respond(HttpStatusCode.Accepted, mapOf("status" to "queued"))
    }

    get("/kafka/messages") {
        call.respond(consumerService.lastMessages)
    }
}

