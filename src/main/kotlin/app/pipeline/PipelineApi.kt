package com.example.app.pipeline

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.pipeline() {
    get("/pipeline") {
        call.respondText(PipelineUi.html(), ContentType.Text.Html)
    }

    get("/api/pipeline/overview") {
        call.respond(PipelineService.pipelineOverview())
    }

    get("/api/pipeline/messages") {
        val topic = call.request.queryParameters["topic"] ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "topic query parameter is required"),
        )
        call.respond(PipelineService.recentMessages(topic))
    }

    post("/api/pipeline/topics/ensure") {
        PipelineService.ensureTopics()
        call.respond(mapOf("status" to "ok", "topics" to PipelineService.uiTopics()))
    }

    post("/api/pipeline/connect/register") {
        call.respond(PipelineService.registerConnector())
    }

    post("/api/pipeline/events/manual") {
        val request = call.receive<ManualEventRequest>()
        call.respond(PipelineService.sendManualEvent(request))
    }

    post("/api/pipeline/clickhouse/query") {
        val request = call.receive<QueryRequest>()
        call.respond(PipelineService.queryClickHouse(request.sql))
    }
}
