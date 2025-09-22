package com.example.app.books.DAO

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable

object BookLinksTable : LongIdTable("book_link") {
    val mongoID = varchar("mongo_id", 255)
}

class BookLinkEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BookLinkEntity>(BookLinksTable)
    var mongoID by BookLinksTable.mongoID

    val bookCopies by BookCopyEntity referrersOn BookCopiesTable.bookLink
}