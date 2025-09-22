package com.example.app.users.clients

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

fun Route.clients() {

    post("/clients") {
        val client = call.receive<ClientRegisterRequest>()
        call.respond(ClientService.createClient(client))
    }

    get("/clients/{id}") {
        val id = call.parameters["id"]!!.toLong()
        call.respond(ClientService.getClient(id))
    }

    get("/clients") {
        call.respond(ClientService.getAll())
    }

    patch("/clients/{id}") {
        val client = call.receive<ClientUpdateRequest>()
        val id = call.parameters["id"]!!.toLong()
        call.respond(ClientService.updateClient(id, client))
    }

    delete("/clients/{id}") {
        val id = call.parameters["id"]!!.toLong()
        ClientService.deleteClient(id)
        call.respondText { "OK" }
    }
}