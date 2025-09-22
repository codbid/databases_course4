package com.example.app.users.clients

import com.example.app.users.clients.DAO.ClientEntity
import com.example.app.users.clients.DTO.ClientRegisterRequest
import com.example.app.users.clients.DTO.ClientResponse
import com.example.app.users.clients.DTO.ClientUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction

object ClientService {

    suspend fun createClient(client: ClientRegisterRequest) = withContext(Dispatchers.IO) {
        transaction {
            ClientEntity.new {
                name = client.name
                email = client.email
                city = client.city
            }
        }
    }

    suspend fun getClient(id: Long): ClientResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = ClientEntity.findById(id) ?: throw Exception("Client not found")
            ClientResponse(entity)
        }
    }

    suspend fun getAll(): List<ClientResponse> = withContext(Dispatchers.IO) {
        transaction { ClientEntity.all().map { entity -> ClientResponse(entity) }.toList() }
    }

    suspend fun updateClient(id: Long, client: ClientUpdateRequest): ClientResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = ClientEntity.findById(id) ?: throw Exception("Client not found")
            client.name?.let { entity.name = it }
            client.email?.let { entity.email = it }
            client.city?.let { entity.city = it }
            ClientResponse(entity)
        }
    }

    suspend fun deleteClient(id: Long) = withContext(Dispatchers.IO) {
        transaction { ClientEntity.findById(id)?.delete() }
    }
}