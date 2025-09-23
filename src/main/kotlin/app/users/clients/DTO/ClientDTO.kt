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
    val id: Long,
    val name: String,
    val email: String,
    val city: String,
    val createdAt: String
)

fun ClientEntity.toResponse() = ClientResponse(
    id = id.value,
    name = name,
    email = email,
    city = city,
    createdAt = createdAt.toString()
)