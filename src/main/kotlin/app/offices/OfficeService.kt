package com.example.app.offices

import com.example.app.offices.DAO.OfficeEntity
import com.example.app.offices.DTO.OfficeCreateRequest
import com.example.app.offices.DTO.OfficeResponse
import com.example.app.offices.DTO.OfficeUpdateRequest
import com.example.app.users.clients.DAO.ClientEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction

object OfficeService {

    suspend fun createOffice(request: OfficeCreateRequest): OfficeResponse = withContext(Dispatchers.IO) {
        transaction {
            val office = OfficeEntity.new {
                name = request.name
                address = request.address
                workingTime = request.workingTime
            }
            OfficeResponse(office)
        }
    }

    suspend fun getOffice(id: Long): OfficeResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = OfficeEntity.findById(id) ?: throw Exception("Office not found")
            OfficeResponse(entity)
        }
    }

    suspend fun getAll(): List<OfficeResponse> = withContext(Dispatchers.IO) {
        transaction { OfficeEntity.all().map { entity -> OfficeResponse(entity) }.toList() }
    }

    suspend fun updateOffice(id: Long, request: OfficeUpdateRequest): OfficeResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = OfficeEntity.findById(id) ?: throw Exception("Office not found")
            request.name?.let { entity.name = it }
            request.address?.let { entity.address = it }
            request.workingTime?.let { entity.workingTime = it }
            OfficeResponse(entity)
        }
    }

    suspend fun deleteOffice(id: Long) = withContext(Dispatchers.IO) {
        transaction {
            val entity = OfficeEntity.findById(id) ?: throw Exception("Office not found")
            OfficeEntity.findById(id)?.delete()
        }
    }
}