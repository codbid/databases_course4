package com.example.app.offices

import com.example.app.offices.DAO.OfficeEntity
import com.example.app.offices.DTO.OfficeCreateRequest
import com.example.app.offices.DTO.OfficeResponse
import com.example.app.offices.DTO.OfficeUpdateRequest
import com.example.app.offices.DTO.toResponse
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

            return@transaction office.toResponse()
        }
    }

    suspend fun getOffice(id: Long): OfficeResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = OfficeEntity.findById(id) ?: throw Exception("Office not found")

            return@transaction entity.toResponse()
        }
    }

    suspend fun getAll(): List<OfficeResponse> = withContext(Dispatchers.IO) {
        transaction { return@transaction OfficeEntity.all().map { entity -> entity.toResponse() }.toList() }
    }

    suspend fun updateOffice(id: Long, request: OfficeUpdateRequest): OfficeResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = OfficeEntity.findById(id) ?: throw Exception("Office not found")
            request.name?.let { entity.name = it }
            request.address?.let { entity.address = it }
            request.workingTime?.let { entity.workingTime = it }

            return@transaction entity.toResponse()
        }
    }

    suspend fun deleteOffice(id: Long) = withContext(Dispatchers.IO) {
        transaction { return@transaction OfficeEntity.findById(id)?.delete() }
    }
}