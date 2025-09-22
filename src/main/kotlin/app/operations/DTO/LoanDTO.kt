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

data class LoanResponse(val entity: LoanEntity) {
    val id = entity.id.value
    val bookCopyID = entity.bookCopy.id.value
    val clientID = entity.client.id.value
    val status = entity.status
    val startDate = entity.startDate
    val endDate = entity.endDate
}