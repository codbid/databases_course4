package com.example.app.books.authors

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.authors() {

    post("/authors") {
        val request = call.receive<String>()
        call.respond(AuthorService.createAuthor(request))
    }

    get("/authors") {
        call.respond(AuthorService.getAuthors())
    }
}