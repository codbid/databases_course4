package com.example.app.operations.DAO

import com.example.app.operations.FineStatus
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable

object FinesTable : LongIdTable("fines") {
    val loan = reference("loan_id", LoansTable)
    val amount = decimal("amount", precision = 5, scale = 2)
    val status = enumerationByName("status", 255, FineStatus::class)
    val createdAt = datetime("created_at")
}

class FineEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<FineEntity>(FinesTable)

    var loan by LoanEntity referencedOn FinesTable.loan
    var amount by FinesTable.amount
    var status by FinesTable.status
    var createdAt by FinesTable.createdAt
}