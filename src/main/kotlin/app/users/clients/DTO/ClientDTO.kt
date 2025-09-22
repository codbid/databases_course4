package com.example.app.users.clients.DTO

import com.example.app.users.clients.DAO.ClientEntity

data class ClientRegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val city: String,
)

data class ClientUpdateRequest(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val city: String? = null,
)

data class ClientResponse(
    val client: ClientEntity
) {
    val id: Long = client.id.value
    val name: String = client.name
    val email: String = client.email
    val city: String = client.city
    val createdAt: String = client.createdAt.toString()
}