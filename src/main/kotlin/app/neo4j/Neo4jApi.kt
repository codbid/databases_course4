package com.example.app.neo4j

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.neo4j() {
    get("/neo4j/stats") {
        call.respond(Neo4jService.getGraphStats())
    }
    get("/neo4j/similar-readers/{clientId}") {
        val clientId = call.parameters["clientId"]!!
        call.respond(Neo4jService.getSimilarReaders(clientId))
    }
}
