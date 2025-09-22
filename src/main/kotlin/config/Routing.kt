package com.example.config

import com.example.app.books.books
import com.example.app.offices.offices
import com.example.app.operations.operations
import com.example.app.users.clients.clients
import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        route("/api") {
            clients()
            offices()
            books()
            operations()
        }

        openAPI(
            path = "openapi",
            swaggerFile = "openapi/documentation.yaml",
        )
        swaggerUI(
            path = "/swagger",
            swaggerFile = "openapi",
        )
    }
}
