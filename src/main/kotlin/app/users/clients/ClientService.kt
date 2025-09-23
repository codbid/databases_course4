package com.example.app.users.clients

import com.example.app.users.clients.DAO.ClientEntity
import com.example.app.users.clients.DTO.ClientRegisterRequest
import com.example.app.users.clients.DTO.ClientResponse
import com.example.app.users.clients.DTO.ClientUpdateRequest
import com.example.app.users.clients.DTO.toResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction

object ClientService {

    suspend fun createClient(client: ClientRegisterRequest): ClientResponse = withContext(Dispatchers.IO) {
        transaction {
            return@transaction ClientEntity.new {
                name = client.name
                email = client.email
                password = client.password
                city = client.city
            }.toResponse()
        }
    }

    suspend fun getClient(id: Long): ClientResponse = withContext(Dispatchers.IO) {
        transaction {
            return@transaction ClientEntity.findById(id)?.toResponse() ?: throw Exception("Client not found")
        }
    }

    suspend fun getAll(): List<ClientResponse> = withContext(Dispatchers.IO) {
        transaction { return@transaction ClientEntity.all().map { entity -> entity.toResponse() }.toList() }
    }

    suspend fun updateClient(id: Long, client: ClientUpdateRequest): ClientResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = ClientEntity.findById(id) ?: throw Exception("Client not found")
            client.name?.let { entity.name = it }
            client.email?.let { entity.email = it }
            client.city?.let { entity.city = it }

            return@transaction entity.toResponse()
        }
    }

    suspend fun deleteClient(id: Long) = withContext(Dispatchers.IO) {
        transaction { return@transaction ClientEntity.findById(id)?.delete() }
    }
}