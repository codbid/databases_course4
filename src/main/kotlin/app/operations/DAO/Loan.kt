package com.example.app.operations.DAO

import com.example.app.books.DAO.BookCopiesTable
import com.example.app.books.DAO.BookCopyEntity
import com.example.app.operations.LoanStatus
import com.example.app.users.clients.DAO.ClientEntity
import com.example.app.users.clients.DAO.ClientsTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable

object LoansTable : LongIdTable("loans") {
    val bookCopy = reference("book_copy_id", BookCopiesTable)
    val client = reference("client_id", ClientsTable)
    val status = enumerationByName("status", 255, LoanStatus::class)
    val startDate = datetime("starts_at")
    val endDate = datetime("ends_at")
}

class LoanEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<LoanEntity>(LoansTable)

    var bookCopy by BookCopyEntity referencedOn LoansTable.bookCopy
    var client by ClientEntity referencedOn LoansTable.client
    var status by LoansTable.status
    var startDate by LoansTable.startDate
    var endDate by LoansTable.endDate
}