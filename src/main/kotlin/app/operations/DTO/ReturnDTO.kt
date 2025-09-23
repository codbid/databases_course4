package com.example.app.operations.DTO

import com.example.app.operations.DAO.ReturnEntity

data class ReturnCreateRequest(
    val loanID: Long
)

data class ReturnResponse(
    val id: Long,
    val loanID: Long,
    val returnDate: String
)

fun ReturnEntity.toResponse() = ReturnResponse(
    id = this.id.value,
    loanID = this.loan.id.value,
    returnDate = this.returnDate.toString()
)