package com.example.app.operations

import com.example.app.books.DAO.BookCopyEntity
import com.example.app.operations.DAO.FineEntity
import com.example.app.operations.DAO.LoanEntity
import com.example.app.operations.DAO.ReservationEntity
import com.example.app.operations.DAO.ReturnEntity
import com.example.app.operations.DTO.FineCreateRequest
import com.example.app.operations.DTO.FineResponse
import com.example.app.operations.DTO.FineUpdateStatusRequest
import com.example.app.operations.DTO.LoanCreateRequest
import com.example.app.operations.DTO.LoanResponse
import com.example.app.operations.DTO.LoanUpdateRequest
import com.example.app.operations.DTO.ReservationCreateRequest
import com.example.app.operations.DTO.ReservationResponse
import com.example.app.operations.DTO.ReturnCreateRequest
import com.example.app.operations.DTO.ReturnResponse
import com.example.app.users.clients.DAO.ClientEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object OperationService {
    suspend fun createFine(request: FineCreateRequest): FineResponse = withContext(Dispatchers.IO) {
        transaction {
            val loanEntity = LoanEntity.findById(request.loanID) ?: throw Exception("Loan not found")

            val entity = FineEntity.new {
                loan = loanEntity
                amount = request.amount
                status = FineStatus.PENDING
            }

            return@transaction FineResponse(entity)
        }
    }

    suspend fun getFine(id: Long): FineResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = FineEntity.findById(id) ?: throw Exception("Fine not found")

            return@transaction FineResponse(entity)
        }
    }

    suspend fun updateFineStatus(id: Long, request: FineUpdateStatusRequest): FineResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = FineEntity.findById(id) ?: throw Exception("Fine not found")
            entity.status = request.status

            return@transaction FineResponse(entity)
        }
    }

    suspend fun deleteFine(id: Long) = withContext(Dispatchers.IO) {
        transaction {
            val entity = FineEntity.findById(id) ?: throw Exception("Fine not found")
            entity.delete()
        }
    }

    suspend fun createLoan(request: LoanCreateRequest) = withContext(Dispatchers.IO) {
        transaction {
            val bookCopyEntity = BookCopyEntity.findById(request.bookCopyID) ?: throw Exception("BookCopy not found")
            val clientEntity = ClientEntity.findById(request.clientID) ?: throw Exception("Client not found")

            val entity = LoanEntity.new {
                bookCopy = bookCopyEntity
                client = clientEntity
                status = LoanStatus.LOANED
                startDate = DateTime.now()
                endDate = DateTime.now().plusDays(request.durationInDays)
            }

            return@transaction LoanResponse(entity)
        }
    }

    suspend fun getLoan(id: Long): LoanResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = LoanEntity.findById(id) ?: throw Exception("Loan not found")

            return@transaction LoanResponse(entity)
        }
    }

    suspend fun updateLoan(id: Long, request: LoanUpdateRequest) = withContext(Dispatchers.IO) {
        transaction {
            val entity = LoanEntity.findById(id) ?: throw Exception("Loan not found")
            request.status?.let { entity.status = it }
            request.durationInDays?.let {
                entity.startDate = DateTime.now()
                entity.endDate = DateTime.now().plusDays(it)
            }

            return@transaction LoanResponse(entity)
        }
    }

    suspend fun deleteLoan(id: Long) = withContext(Dispatchers.IO) {
        transaction {
            val entity = LoanEntity.findById(id) ?: throw Exception("Loan not found")
            entity.delete()
        }
    }

    suspend fun createReservation(request: ReservationCreateRequest): ReservationResponse = withContext(Dispatchers.IO) {
        transaction {
            val bookCopyEntity = BookCopyEntity.findById(request.bookCopyID) ?: throw Exception("BookCopy not found")
            val clientEntity = ClientEntity.findById(request.clientID) ?: throw Exception("Client not found")

            val entity = ReservationEntity.new {
                bookCopy = bookCopyEntity
                client = clientEntity
                startDate = DateTime.now()
                endDate = DateTime.now().plusDays(request.durationInDays)
            }

            return@transaction ReservationResponse(entity)
        }
    }

    suspend fun getReservation(id: Long): ReservationResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = ReservationEntity.findById(id) ?: throw Exception("Reservation not found")

            return@transaction ReservationResponse(entity)
        }
    }

    suspend fun deleteReservation(id: Long) = withContext(Dispatchers.IO) {
        transaction {
            val entity = ReservationEntity.findById(id) ?: throw Exception("Reservation not found")
            entity.delete()
        }
    }

    suspend fun createReturn(request: ReturnCreateRequest): ReturnResponse = withContext(Dispatchers.IO) {
        transaction {
            val loanEntity = LoanEntity.findById(request.loanID) ?: throw Exception("Loan not found")

            val entity = ReturnEntity.new {
                loan = loanEntity
            }

            return@transaction ReturnResponse(entity)
        }
    }

    suspend fun getReturn(id: Long): ReturnResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = ReturnEntity.findById(id) ?: throw Exception("Return not found")

            return@transaction ReturnResponse(entity)
        }
    }

    suspend fun deleteReturn(id: Long) = withContext(Dispatchers.IO) {
        transaction {
            val entity = ReturnEntity.findById(id) ?: throw Exception("Return not found")
            entity.delete()
        }
    }
}