package com.example.app.books

import com.example.app.books.DAO.BookCopyEntity
import com.example.app.books.DAO.BookLinkEntity
import com.example.app.books.DAO.BookLinksTable
import com.example.app.books.DTO.BookCopyCreateRequest
import com.example.app.books.DTO.BookCopyResponse
import com.example.app.books.DTO.BookCopyUpdateRequest
import com.example.app.books.DTO.BookCreateRequest
import com.example.app.books.DTO.BookResponse
import com.example.app.books.DTO.BookUpdateRequest
import com.example.app.books.DTO.BooksCountGroupByOfficeResponse
import com.example.app.books.DTO.BooksCountGroupByOfficeResponseRanked
import com.example.app.books.DTO.toBookResponse
import com.example.app.books.DTO.toResponse
import com.example.app.offices.DAO.OfficeEntity
import com.example.config.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object BookService {

    suspend fun createBook(request: BookCreateRequest): BookResponse = withContext(Dispatchers.IO) {
        val mongoBookCollection = DatabaseFactory.mongo.getCollection("books")

        val doc = Document(
            mapOf(
                "title" to request.title,
                "author" to request.author,
                "genre" to request.genre,
                "year" to request.year,
                "description" to request.description,
                "tags" to (request.tags ?: emptyList()),
                "isbnNumber" to request.isbnNumber
            )
        )
        val insertResult = mongoBookCollection.insertOne(doc)
        val mongoId = insertResult.insertedId?.asObjectId()?.value
            ?: (doc.getObjectId("_id"))
            ?: throw IllegalStateException("Mongo _id not generated")

        transaction {
            BookLinkEntity.new { mongoID = mongoId.toString() }
            return@transaction doc.toBookResponse()
        }
    }

    suspend fun getBook(bookID: Long): BookResponse = withContext(Dispatchers.IO) {
        val mongoBookCollection = DatabaseFactory.mongo.getCollection("books")
        val doc = mongoBookCollection.find(Document("mongoId", getMongoId(bookID))).firstOrNull()
        return@withContext doc?.toBookResponse() ?: throw Exception("Book not found")
    }

    suspend fun getAllBooks(): List<BookResponse> = withContext(Dispatchers.IO) {
        val mongoBookCollection = DatabaseFactory.mongo.getCollection("books")
        val docs = mongoBookCollection.find().toList()
        return@withContext docs.map { it.toBookResponse() }
    }

    suspend fun updateBook(bookID: Long, request: BookUpdateRequest): BookResponse = withContext(Dispatchers.IO) {
        val mongoBookCollection = DatabaseFactory.mongo.getCollection("books")
        val mongoID = getMongoId(bookID)
        mongoBookCollection.updateOne(
            Document("mongoId", mongoID),
            Document(
                "\$set", Document(
                    mapOf(
                        "title" to request.title,
                        "author" to request.author,
                        "genre" to request.genre,
                        "year" to request.year,
                        "description" to request.description,
                        "tags" to (request.tags ?: emptyList()),
                        "isbnNumber" to request.isbnNumber
                    )
                )
            )
        )
        val doc = mongoBookCollection.find(Document("mongoId", mongoID)).firstOrNull()
        return@withContext doc?.toBookResponse() ?: throw Exception("Error updating book")
    }

    suspend fun deleteBook(bookID: Long) = withContext(Dispatchers.IO) {
        val mongoBookCollection = DatabaseFactory.mongo.getCollection("books")
        mongoBookCollection.deleteOne(Document("mongoId", getMongoId(bookID)))
    }

    suspend private fun getMongoId(bookID: Long) = withContext(Dispatchers.IO) {
        transaction {
            val mongoId = BookLinksTable
                .slice(BookLinksTable.mongoID)
                .select { BookLinksTable.id eq bookID }
                .limit(1)
            return@transaction mongoId
        }
    }

    suspend fun createBookCopy(bookID: Long, request: BookCopyCreateRequest): BookCopyResponse = withContext(Dispatchers.IO) {
        transaction {
            val bookLinkEntity = BookLinkEntity.findById(bookID) ?: throw Exception("Book not found")
            val officeEntity = OfficeEntity.findById(request.officeID) ?: throw Exception("Office not found")

            val entity = BookCopyEntity.new {
                bookLink = bookLinkEntity
                office = officeEntity
                status = request.status
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getBookCopy(copyID: Long): BookCopyResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = BookCopyEntity.findById(copyID) ?: throw Exception("BookCopy not found")

            return@transaction entity.toResponse()
        }
    }

    suspend fun updateBookCopy(copyID: Long, request: BookCopyUpdateRequest): BookCopyResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = BookCopyEntity.findById(copyID) ?: throw Exception("BookCopy not found")
            request.officeID?.let { entity.office = OfficeEntity.findById(it) ?: throw Exception("Office not found") }
            request.status?.let { entity.status = it }

            return@transaction entity.toResponse()
        }
    }

    suspend fun deleteBookCopy(copyID: Long) = withContext(Dispatchers.IO) {
        transaction {
            val entity = BookCopyEntity.findById(copyID) ?: throw Exception("BookCopy not found")
            entity.delete()
        }
    }

    suspend fun getAvailableCopiesGroupedByOffice(): List<BooksCountGroupByOfficeResponse> = withContext(Dispatchers.IO) {
        transaction { // agr 2 tab
            val sql = """SELECT o.id AS office_id, o.name, COUNT(*) AS available_copies
                        FROM book_copies c
                        JOIN offices o ON o.id = c.office_id
                        WHERE c.status = 'AVAILABLE'
                        GROUP BY o.id, o.name
                        ORDER BY available_copies DESC
                    """.trimIndent()

            val result = mutableListOf<BooksCountGroupByOfficeResponse>()

            exec(sql) { rs ->
                while (rs.next()) {
                    result.add(
                        BooksCountGroupByOfficeResponse(
                            rs.getLong("office_id"),
                            rs.getString("name"),
                            rs.getInt("available_copies")
                        )
                    )
                }
            }

            return@transaction result
        }
    }

    suspend fun getRankingAvailableCopiesGroupedByOffice(): List<BooksCountGroupByOfficeResponseRanked> = withContext(Dispatchers.IO) {
        transaction { // win 2 tab
            val sql = """SELECT o.id, o.name,
                               COUNT(*) FILTER (WHERE c.status = 'AVAILABLE') AS available_copies,
                               RANK() OVER (ORDER BY COUNT(*) FILTER (WHERE c.status = 'AVAILABLE') DESC) AS rank_by_available
                        FROM offices o
                        LEFT JOIN book_copies c ON c.office_id = o.id
                        GROUP BY o.id, o.name
                        ORDER BY rank_by_available, o.name;
                    """.trimIndent()

            val result = mutableListOf<BooksCountGroupByOfficeResponseRanked>()

            exec(sql) { rs ->
                while (rs.next()) {
                    result.add(
                        BooksCountGroupByOfficeResponseRanked(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getInt("available_copies"),
                            rs.getInt("rank_by_available")
                        )
                    )
                }
            }

            return@transaction result
        }
    }
}