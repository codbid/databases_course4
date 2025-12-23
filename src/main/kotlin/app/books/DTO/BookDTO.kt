package com.example.app.books.DTO

import com.example.app.books.BookCopyStatus
import com.example.app.books.DAO.BookCopyEntity
import com.example.app.books.DAO.BookLinksTable
import org.bson.Document
import org.jetbrains.exposed.sql.select
import org.joda.time.DateTime

data class BookCreateRequest(
    val title: String,
    val genre: String?,
    val year: Int,
    val description: String?,
    val tags: List<String>?,
    val isbnNumber: String
)

data class BookUpdateRequest(
    val title: String? = null,
    val genre: String?? = null,
    val year: Int? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val isbnNumber: String? = null
)

data class BookResponse(
    val id: Long,
    val title: String,
    val genre: String?,
    val year: Int,
    val description: String?,
    val tags: List<String>?,
    val isbnNumber: String
)

fun Document.toBookResponse(): BookResponse {
    val bookID = this.getObjectId("_id").toString()
    val bookLinkID = BookLinksTable
        .slice(BookLinksTable.id)
        .select { BookLinksTable.mongoID eq bookID }
        .limit(1)
        .map { it[BookLinksTable.id].value }
        .firstOrNull()
        ?: throw Exception("BookLink not found for book id: $bookID")

    return BookResponse(
        id = bookLinkID,
        title = this.getString("title"),
        genre = this.getString("genre"),
        year = this.getInteger("year"),
        description = this.getString("description"),
        tags = this.getList("tags", String::class.java),
        isbnNumber = this.getString("isbnNumber")
    )
}

data class BookCopyCreateRequest(
    val officeID: Long,
    val status: BookCopyStatus = BookCopyStatus.AVAILABLE
)

data class BookCopyUpdateRequest(
    val officeID: Long? = null,
    val status: BookCopyStatus? = null
)

data class BookCopyResponse(
    val id: Long,
    val bookID: Long,
    val officeID: Long,
    val status: String,
    val createdAt: String
)

fun BookCopyEntity.toResponse() = BookCopyResponse(
    id = id.value,
    bookID = bookLink.id.value,
    officeID = office.id.value,
    status = status.toString(),
    createdAt = createdAt.toString()
)

data class BooksCountGroupByOfficeResponse(
    val officeID: Long,
    val officeName: String,
    val count: Int
)

data class BooksCountGroupByOfficeResponseRanked(
    val officeID: Long,
    val officeName: String,
    val count: Int,
    val rank: Int
)