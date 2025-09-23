package com.example.app.operations.DTO

import com.example.app.operations.DAO.LoanEntity
import com.example.app.operations.LoanStatus

data class LoanCreateRequest(
    val bookCopyID: Long,
    val clientID: Long,
    val durationInDays: Int
)

data class LoanUpdateRequest(
    val durationInDays: Int? = null,
    val status: LoanStatus? = null
)

data class LoanResponse(
    val id: Long,
    val bookCopyID: Long,
    val clientID: Long,
    val status: LoanStatus,
    val startDate: String,
    val endDate: String
)

fun LoanEntity.toResponse() = LoanResponse(
    id = id.value,
    bookCopyID = bookCopy.id.value,
    clientID = client.id.value,
    status = status,
    startDate = startDate.toString(),
    endDate = endDate.toString()
)



data class LoansCountGroupByOfficeResponse(
    val officeID: Long,
    val officeName: String,
    val count: Int
)