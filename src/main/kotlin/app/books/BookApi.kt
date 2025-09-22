package com.example.app.books

import com.example.app.books.DTO.BookCopyCreateRequest
import com.example.app.books.DTO.BookCopyUpdateRequest
import com.example.app.books.DTO.BookCreateRequest
import com.example.app.books.DTO.BookUpdateRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlin.text.toLong

fun Route.books() {

    post("/books") {
        val request = call.receive<BookCreateRequest>()
        call.respond(BookService.createBook(request))
    }

    get("/books/{id}") {
        val id = call.parameters["id"]!!.toLong()
        call.respond(BookService.getBook(id))
    }

    patch("/books/{id}") {
        val request = call.receive<BookUpdateRequest>()
        val id = call.parameters["id"]!!.toLong()
        call.respond(BookService.updateBook(id, request))
    }

    delete("/books/{id}") {
        val id = call.parameters["id"]!!.toLong()
        BookService.deleteBook(id)
        call.respondText { "OK" }
    }

    post("/books/{id}/copies") {
        val office = call.receive<BookCopyCreateRequest>()
        val id = call.parameters["id"]!!.toLong()
        call.respond(BookService.createBookCopy(id, office))
    }

    get("/books/{id}/copies") {
        val id = call.parameters["id"]!!.toLong()
        call.respond(BookService.getBookCopy(id))
    }

    patch("/books/{id}/copies") {
        val request = call.receive<BookCopyUpdateRequest>()
        val id = call.parameters["id"]!!.toLong()
        call.respond(BookService.updateBookCopy(id, request))
    }

    delete("/books/{id}/copies") {
        val id = call.parameters["id"]!!.toLong()
        BookService.deleteBookCopy(id)
        call.respondText { "OK" }
    }
}