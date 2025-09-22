package com.example.app.operations.DTO

import com.example.app.operations.DAO.ReturnEntity

data class ReturnCreateRequest(
    val loanID: Long
)

data class ReturnResponse(val entity: ReturnEntity) {
    val id = entity.id
    val loanID = entity.loan.id.value
    val returnDate = entity.returnDate
}