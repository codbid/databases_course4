package com.example.app.operations.DTO

import com.example.app.operations.DAO.FineEntity
import com.example.app.operations.FineStatus
import java.math.BigDecimal

data class FineCreateRequest (
    val loanID: Long,
    val amount: BigDecimal
)

data class FineUpdateStatusRequest (
    val status: FineStatus
)

data class FineResponse (val entity: FineEntity) {
    val id = entity.id.value
    val loanID = entity.loan.id.value
    val amount = entity.amount
    val status = entity.status
    val createdAt = entity.createdAt
}