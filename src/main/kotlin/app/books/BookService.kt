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
import com.example.app.books.DTO.toBookResponse
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
            val entity = BookLinkEntity.new { mongoID = mongoId.toString() }
        }

        return@withContext doc.toBookResponse()
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
                bookLink = bookLinkEntity   // тут нужен именно Entity
                office = officeEntity
                status = request.status
            }

            return@transaction BookCopyResponse(entity)
        }
    }

    suspend fun getBookCopy(id: Long): BookCopyResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = BookCopyEntity.findById(id) ?: throw Exception("BookCopy not found")
            return@transaction BookCopyResponse(entity)
        }
    }

    suspend fun updateBookCopy(id: Long, request: BookCopyUpdateRequest): BookCopyResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = BookCopyEntity.findById(id) ?: throw Exception("BookCopy not found")
            request.officeID?.let { entity.office = OfficeEntity.findById(it) ?: throw Exception("Office not found") }
            request.status?.let { entity.status = it }
            return@transaction BookCopyResponse(entity)
        }
    }

    suspend fun deleteBookCopy(id: Long) = withContext(Dispatchers.IO) {
        transaction {
            val entity = BookCopyEntity.findById(id) ?: throw Exception("BookCopy not found")
            entity.delete()
        }
    }
}