package com.example.app.test

import com.example.app.books.MongoAssignmentService
import com.example.app.operations.DTO.FineCreateRequest
import com.example.app.operations.OperationService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlin.text.toLong

fun Route.test() {

    get("/mongo-test/setup") {
        MongoAssignmentService.clearAll()

        MongoAssignmentService.seedData()
        val before = MongoAssignmentService.runPerformanceTest()

        MongoAssignmentService.createIndexes()

        val after = MongoAssignmentService.runPerformanceTest()

        call.respond(
            mapOf(
                "before_indexes" to before,
                "after_indexes" to after,
                "status" to "Done! Data seeded and indexes created."
            )
        )
    }
}