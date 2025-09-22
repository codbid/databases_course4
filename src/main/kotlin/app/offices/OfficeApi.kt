package com.example.app.offices

import com.example.app.offices.DTO.OfficeCreateRequest
import com.example.app.offices.DTO.OfficeUpdateRequest
import com.example.app.users.clients.ClientService
import com.example.app.users.clients.DTO.ClientRegisterRequest
import com.example.app.users.clients.DTO.ClientUpdateRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlin.text.toLong

fun Route.offices() {

    post("/offices") {
        val office = call.receive<OfficeCreateRequest>()
        call.respond(OfficeService.createOffice(office))
    }

    get("/offices/{id}") {
        val id = call.parameters["id"]!!.toLong()
        call.respond(OfficeService.getOffice(id))
    }

    get("/offices") {
        call.respond(OfficeService.getAll())
    }

    patch("/offices/{id}") {
        val office = call.receive<OfficeUpdateRequest>()
        val id = call.parameters["id"]!!.toLong()
        call.respond(OfficeService.updateOffice(id, office))
    }

    delete("/offices/{id}") {
        val id = call.parameters["id"]!!.toLong()
        OfficeService.deleteOffice(id)
        call.respondText { "OK" }
    }
}