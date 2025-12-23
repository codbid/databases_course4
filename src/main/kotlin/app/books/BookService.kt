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
import com.example.app.util.toApi
import com.example.config.DatabaseFactory
import com.mongodb.client.model.Accumulators.first
import com.mongodb.client.model.Accumulators.push
import com.mongodb.client.model.Aggregates.group
import com.mongodb.client.model.Aggregates.lookup
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Aggregates.unwind
import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object BookService {

    suspend fun createBook(request: BookCreateRequest): BookResponse = withContext(Dispatchers.IO) {
        val mongoBookCollection = DatabaseFactory.mongo.getCollection("books")

        val doc = Document(
            mapOf(
                "title" to request.title,
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
        val mongoId = ObjectId(getMongoId(bookID))
        val doc = mongoBookCollection.find(Document("_id", mongoId)).firstOrNull()
        return@withContext doc?.toBookResponse() ?: throw Exception("Book not found")
    }

    suspend fun getAllBooks(): List<BookResponse> = withContext(Dispatchers.IO) {
        val mongoBookCollection = DatabaseFactory.mongo.getCollection("books")
        val docs = mongoBookCollection.find().toList()
        return@withContext docs.map { it.toBookResponse() }
    }

    suspend fun updateBook(bookID: Long, request: BookUpdateRequest): BookResponse = withContext(Dispatchers.IO) {
        val mongoBookCollection = DatabaseFactory.mongo.getCollection("books")
        val mongoId = ObjectId(getMongoId(bookID))

        mongoBookCollection.updateOne(
            Document("_id", mongoId),
            Document(
                "\$set", Document(
                    mapOf(
                        "title" to request.title,
                        "genre" to request.genre,
                        "year" to request.year,
                        "description" to request.description,
                        "tags" to (request.tags ?: emptyList()),
                        "isbnNumber" to request.isbnNumber
                    )
                )
            )
        )

        val doc = mongoBookCollection.find(Document("_id", mongoId)).firstOrNull()

        return@withContext doc?.toBookResponse() ?: throw Exception("Error updating book")
    }

    suspend fun deleteBook(bookID: Long) = withContext(Dispatchers.IO) {
        val mongoBookCollection = DatabaseFactory.mongo.getCollection("books")
        val mongoId = ObjectId(getMongoId(bookID))
        mongoBookCollection.deleteOne(Document("_id", mongoId))
    }

    suspend private fun getMongoId(bookID: Long): String = withContext(Dispatchers.IO) {
        transaction {
            BookLinksTable
                .slice(BookLinksTable.mongoID)
                .select { BookLinksTable.id eq bookID }
                .firstOrNull()
                ?.get(BookLinksTable.mongoID)
                ?: throw Exception("MongoID not found for book $bookID")
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

    suspend fun linkBookAuthor(bookId: String, authorId: String) {
        DatabaseFactory.mongo.getCollection("book_authors")
            .insertOne(
                Document("bookId", ObjectId(bookId))
                    .append("authorId", ObjectId(authorId))
                    .append("role", "author")
            )
    }

    suspend fun getBookWithAuthors(bookMongoId: String): Map<String, Any?> =
        withContext(Dispatchers.IO) {

            val booksCol = DatabaseFactory.mongo.getCollection("books")

            val pipeline = listOf(
                match(eq("_id", ObjectId(bookMongoId))),

                lookup(
                    "book_authors",
                    "_id",
                    "bookId",
                    "links"
                ),

                unwind("\$links"),

                lookup(
                    "authors",
                    "links.authorId",
                    "_id",
                    "author"
                ),

                unwind("\$author"),

                group(
                    "\$_id",
                    first("title", "\$title"),
                    first("isbnNumber", "\$isbnNumber"),
                    first("year", "\$year"),
                    first("description", "\$description"),
                    first("tags", "\$tags"),
                    push(
                        "authors",
                        Document(
                            mapOf(
                                "_id" to "\$author._id",
                                "name" to "\$author.name",
                                "bio" to "\$author.bio"
                            )
                        )
                    )
                )
            )

            val result = booksCol.aggregate(pipeline).firstOrNull()
                ?: throw NoSuchElementException("Book not found")

            result.toApi()
        }

}