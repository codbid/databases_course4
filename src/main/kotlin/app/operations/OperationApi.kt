package com.example.app.operations

import com.example.app.books.BookService
import com.example.app.books.DTO.BookCreateRequest
import com.example.app.books.DTO.BookUpdateRequest
import com.example.app.operations.DTO.FineCreateRequest
import com.example.app.operations.DTO.FineUpdateStatusRequest
import com.example.app.operations.DTO.LoanCreateRequest
import com.example.app.operations.DTO.LoanUpdateRequest
import com.example.app.operations.DTO.ReservationCreateRequest
import com.example.app.operations.DTO.ReturnCreateRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlin.text.toLong

fun Route.operations() {

    post("/fines") {
        val request = call.receive<FineCreateRequest>()
        call.respond(OperationService.createFine(request))
    }

    get("/fines/{id}") {
        val id = call.parameters["id"]!!.toLong()
        call.respond(OperationService.getFine(id))
    }

    patch("/fines/{id}") {
        val request = call.receive<FineUpdateStatusRequest>()
        val id = call.parameters["id"]!!.toLong()
        call.respond(OperationService.updateFineStatus(id, request))
    }

    delete("/fines/{id}") {
        val id = call.parameters["id"]!!.toLong()
        OperationService.deleteFine(id)
        call.respondText { "OK" }
    }

    //

    post("/loans") {
        val request = call.receive<LoanCreateRequest>()
        call.respond(OperationService.createLoan(request))
    }

    get("/loans/{id}") {
        val id = call.parameters["id"]!!.toLong()
        call.respond(OperationService.getLoan(id))
    }

    patch("/loans/{id}") {
        val request = call.receive<LoanUpdateRequest>()
        val id = call.parameters["id"]!!.toLong()
        call.respond(OperationService.updateLoan(id, request))
    }

    delete("/loans/{id}") {
        val id = call.parameters["id"]!!.toLong()
        OperationService.deleteLoan(id)
        call.respondText { "OK" }
    }

    //

    post("/reservations") {
        val request = call.receive<ReservationCreateRequest>()
        call.respond(OperationService.createReservation(request))
    }

    get("/reservations/{id}") {
        val id = call.parameters["id"]!!.toLong()
        call.respond(OperationService.getReservation(id))
    }

    delete("/reservations/{id}") {
        val id = call.parameters["id"]!!.toLong()
        OperationService.deleteReservation(id)
        call.respondText { "OK" }
    }

    //

    post("/returns") {
        val request = call.receive<ReturnCreateRequest>()
        call.respond(OperationService.createReturn(request))
    }

    get("/returns/{id}") {
        val id = call.parameters["id"]!!.toLong()
        call.respond(OperationService.getReturn(id))
    }

    delete("/returns/{id}") {
        val id = call.parameters["id"]!!.toLong()
        OperationService.deleteReturn(id)
        call.respondText { "OK" }
    }

}