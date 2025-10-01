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
import com.example.app.operations.DTO.LoansCountGroupByBookRankedResponse
import com.example.app.operations.DTO.LoansCountGroupByClientResponse
import com.example.app.operations.DTO.LoansCountGroupByOfficeResponse
import com.example.app.operations.DTO.ReservationCreateRequest
import com.example.app.operations.DTO.ReservationResponse
import com.example.app.operations.DTO.ReturnCreateRequest
import com.example.app.operations.DTO.ReturnResponse
import com.example.app.operations.DTO.toResponse
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

            return@transaction entity.toResponse()
        }
    }

    suspend fun getFine(id: Long): FineResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = FineEntity.findById(id) ?: throw Exception("Fine not found")

            return@transaction entity.toResponse()
        }
    }

    suspend fun updateFineStatus(id: Long, request: FineUpdateStatusRequest): FineResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = FineEntity.findById(id) ?: throw Exception("Fine not found")
            entity.status = request.status

            return@transaction entity.toResponse()
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
                status = LoanStatus.ACTIVE
                startDate = DateTime.now()
                endDate = DateTime.now().plusDays(request.durationInDays)
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getLoan(id: Long): LoanResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = LoanEntity.findById(id) ?: throw Exception("Loan not found")

            return@transaction entity.toResponse()
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

            return@transaction entity.toResponse()
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

            return@transaction entity.toResponse()
        }
    }

    suspend fun getReservation(id: Long): ReservationResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = ReservationEntity.findById(id) ?: throw Exception("Reservation not found")

            return@transaction entity.toResponse()
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

            return@transaction entity.toResponse()
        }
    }

    suspend fun getReturn(id: Long): ReturnResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = ReturnEntity.findById(id) ?: throw Exception("Return not found")

            return@transaction entity.toResponse()
        }
    }

    suspend fun deleteReturn(id: Long) = withContext(Dispatchers.IO) {
        transaction {
            val entity = ReturnEntity.findById(id) ?: throw Exception("Return not found")
            entity.delete()
        }
    }

    suspend fun getLoansByPeriodGroupedByOffice(start: DateTime, end: DateTime): List<LoansCountGroupByOfficeResponse> = withContext(Dispatchers.IO) {
        transaction { // agr 3 tab
            val sql = """SELECT o.id,
                               MAX(o.name) AS office_name,
                               COUNT(*) AS loans_count
                        FROM loans l
                        JOIN book_copies c ON c.id = l.book_copy_id
                        JOIN offices o     ON o.id = c.office_id
                        WHERE l.starts_at >= '$start' AND l.starts_at < '$end'
                        GROUP BY o.id
                        ORDER BY loans_count DESC;
                            """.trimIndent()

            val result = mutableListOf<LoansCountGroupByOfficeResponse>()

            exec(sql) { rs ->
                while (rs.next()) {
                    result.add(
                        LoansCountGroupByOfficeResponse(
                            rs.getLong("office_id"),
                            rs.getString("office_name"),
                            rs.getInt("loans_count")
                        ))
                }
            }

            return@transaction result
        }
    }

    suspend fun getOverdueActiveLoansGroupedByOffice(): List<LoansCountGroupByOfficeResponse> = withContext(Dispatchers.IO) {
        transaction { // agr 4 tab
            val sql = """SELECT o.id AS office_id, o.name,
                               MAX(o.name) AS office_name,
                               COUNT(*) AS overdue_active
                        FROM loans l
                        LEFT JOIN returns r   ON r.loan_id = l.id
                        JOIN book_copies c    ON c.id = l.book_copy_id
                        JOIN offices o        ON o.id = c.office_id
                        WHERE r.loan_id IS NULL
                          AND l.ends_at < NOW()
                        GROUP BY o.id
                        ORDER BY overdue_active DESC
                    """.trimIndent()

            val result = mutableListOf<LoansCountGroupByOfficeResponse>()

            exec(sql) { rs ->
                while (rs.next()) {
                    result.add(
                        LoansCountGroupByOfficeResponse(
                            rs.getLong("office_id"),
                            rs.getString("office_name"),
                            rs.getInt("overdue_active")
                        ))
                }
            }

            return@transaction result
        }
    }

    suspend fun getRankingOfActiveLoansGroupedByClient(): List<LoansCountGroupByClientResponse> = withContext(Dispatchers.IO) {
        transaction { // win 2 tab
            val sql = """SELECT l.client_id,
                            COUNT(*) FILTER (WHERE r.loan_id IS NULL) AS active_loans, 
                            RANK() OVER (ORDER BY COUNT(*) FILTER (WHERE r.loan_id IS NULL) DESC) AS rank_by_active 
                        FROM loans l 
                        LEFT JOIN returns r ON r.loan_id = l.id 
                        GROUP BY l.client_id;
                    """.trimIndent()

            val result = mutableListOf<LoansCountGroupByClientResponse>()

            exec(sql) { rs ->
                while (rs.next()) {
                    result.add(
                        LoansCountGroupByClientResponse(
                            rs.getLong("client_id"),
                            rs.getInt("active_loans"),
                            rs.getInt("rank_by_active")
                        ))
                }
            }

            return@transaction result
        }

    }

    suspend fun getRankingLoansGroupedByBook(): List<LoansCountGroupByBookRankedResponse> = withContext(Dispatchers.IO) {
        transaction { // win 3 tab
            val sql = """SELECT bl.id AS book_id,
                               COUNT(l.id) AS loans_count,
                               RANK() OVER () AS rank_by_popularity
                        FROM book_link bl
                        LEFT JOIN book_copies c ON c.book_link_id = bl.id
                        LEFT JOIN loans l ON l.book_copy_id = c.id
                        GROUP BY bl.id
                        ORDER BY rank_by_popularity, bl.id;
                    """.trimIndent()

            val result = mutableListOf<LoansCountGroupByBookRankedResponse>()


            exec(sql) { rs ->
                while (rs.next()) {
                    result.add(
                        LoansCountGroupByBookRankedResponse(
                            rs.getLong("book_id"),
                            rs.getInt("loans_count"),
                            rs.getInt("rank_by_popularity")
                        ))
                }
            }

            return@transaction result
        }
    }



}