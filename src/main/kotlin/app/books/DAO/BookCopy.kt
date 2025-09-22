package com.example.app.books.DAO

import com.example.app.books.BookCopyStatus
import com.example.app.offices.DAO.OfficeEntity
import com.example.app.offices.DAO.OfficesTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable

object BookCopiesTable : LongIdTable("book_copies") {
    val bookLink = reference("book_link_id", BookLinksTable)
    val office = reference("office_id", OfficesTable)
    val status = enumerationByName("status", 255, BookCopyStatus::class)
    val createdAt = datetime("created_at")
}

class BookCopyEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BookCopyEntity>(BookCopiesTable)
    var bookLink by BookLinkEntity referencedOn BookCopiesTable.bookLink
    var office by OfficeEntity referencedOn BookCopiesTable.office
    var status by BookCopiesTable.status
    var createdAt by BookCopiesTable.createdAt
}