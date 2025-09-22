package com.example.app.operations.DAO

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable

object ReturnsTable : LongIdTable("returns") {
    val loan = reference("loan_id", LoansTable)
    val returnDate = datetime("returned_at")
}

class ReturnEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ReturnEntity>(ReturnsTable)

    var loan by LoanEntity referencedOn ReturnsTable.loan
    var returnDate by ReturnsTable.returnDate
}