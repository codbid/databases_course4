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

data class FineResponse (
    val id: Long,
    val loanID: Long,
    val amount: BigDecimal,
    val status: FineStatus,
    val createdAt: String
)

fun FineEntity.toResponse() = FineResponse(
    id = id.value,
    loanID = loan.id.value,
    amount = amount,
    status = status,
    createdAt = createdAt.toString()
)