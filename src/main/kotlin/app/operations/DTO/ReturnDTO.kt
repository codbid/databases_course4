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
    id = id.value,
    loanID = loan.id.value,
    returnDate = returnDate.toString()
)