package com.example.app.operations.DAO

import com.example.app.books.DAO.BookCopiesTable
import com.example.app.books.DAO.BookCopyEntity
import com.example.app.users.clients.DAO.ClientEntity
import com.example.app.users.clients.DAO.ClientsTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable

object ReservationsTable : LongIdTable("reservations") {
    val bookCopy = reference("book_copy_id", BookCopiesTable)
    val client = reference("client_id", ClientsTable)
    val startDate = datetime("starts_at")
    val endDate = datetime("ends_at")
}

class ReservationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ReservationEntity>(ReservationsTable)

    var bookCopy by BookCopyEntity referencedOn ReservationsTable.bookCopy
    var client by ClientEntity referencedOn ReservationsTable.client
    var startDate by ReservationsTable.startDate
    var endDate by ReservationsTable.endDate
}