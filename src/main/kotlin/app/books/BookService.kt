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
import com.mongodb.client.model.Aggregates.limit
import com.mongodb.client.model.Aggregates.lookup
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Aggregates.sort
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

        materializeTopAuthorsCache() // кэширую
    }

    suspend fun insertManyBooks(requests: List<BookCreateRequest>): List<Map<String, Any?>> =
        withContext(Dispatchers.IO) {

            val col = DatabaseFactory.mongo.getCollection("books")

            val docs = requests.map {
                Document(
                    mapOf(
                        "title" to it.title,
                        "genre" to it.genre,
                        "year" to it.year,
                        "description" to it.description,
                        "tags" to (it.tags ?: emptyList()),
                        "isbnNumber" to it.isbnNumber,
                        "views" to 0,
                        "editions" to listOf(
                            Document("year", it.year).append("pages", 300)
                        )
                    )
                )
            }

            col.insertMany(docs)
            docs.map { it.toApi() }
        }


    suspend fun updateBookAdvanced(bookMongoId: String): Map<String, Any?> =
        withContext(Dispatchers.IO) {

            val col = DatabaseFactory.mongo.getCollection("books")
            val id = ObjectId(bookMongoId)

            col.updateOne(
                Document("_id", id),
                Document(
                    "\$set", Document("description", "Updated description")
                ).append(
                    "\$inc", Document("views", 1)
                ).append(
                    "\$addToSet", Document("tags", "updated")
                )
            )

            col.find(Document("_id", id)).firstOrNull()?.toApi()
                ?: throw Exception("Book not found")
        }

    suspend fun updateEditionPages(bookMongoId: String, year: Int, pages: Int): Map<String, Any?> =
        withContext(Dispatchers.IO) {

            val col = DatabaseFactory.mongo.getCollection("books")

            col.updateOne(
                Document("_id", ObjectId(bookMongoId)),
                Document(
                    "\$set",
                    Document("editions.\$[e].pages", pages)
                ),
                com.mongodb.client.model.UpdateOptions().arrayFilters(
                    listOf(Document("e.year", year))
                )
            )

            col.find(Document("_id", ObjectId(bookMongoId))).firstOrNull()?.toApi()
                ?: throw Exception("Book not found")
        }

    suspend fun addTagToOldBooks(): Long =
        withContext(Dispatchers.IO) {

            val result = DatabaseFactory.mongo
                .getCollection("books")
                .updateMany(
                    Document("year", Document("\$lt", 1990)),
                    Document("\$addToSet", Document("tags", "old"))
                )

            result.modifiedCount
        }

    suspend fun replaceBook(bookMongoId: String): Map<String, Any?> =
        withContext(Dispatchers.IO) {

            val col = DatabaseFactory.mongo.getCollection("books")
            val id = ObjectId(bookMongoId)

            val newDoc = Document("_id", id)
                .append("title", "Replaced book")
                .append("genre", "replaced")
                .append("year", 2024)
                .append("tags", listOf("replace"))
                .append("views", 0)

            col.replaceOne(Document("_id", id), newDoc)
            newDoc.toApi()
        }

    suspend fun upsertBookByIsbn(isbn: String): Map<String, Any?> =
        withContext(Dispatchers.IO) {

            val col = DatabaseFactory.mongo.getCollection("books")

            col.updateOne(
                Document("isbnNumber", isbn),
                Document(
                    "\$set",
                    Document("title", "Upserted book")
                        .append("year", 2024)
                ),
                com.mongodb.client.model.UpdateOptions().upsert(true)
            )

            col.find(Document("isbnNumber", isbn)).first().toApi()
        }

    suspend fun searchBooks(
        includeTags: List<String> = emptyList(),
        excludeTags: List<String> = emptyList(),
        genresOr: List<String> = emptyList(),
        yearFrom: Int? = null,
        yearTo: Int? = null
    ): List<Map<String, Any?>> =
        withContext(Dispatchers.IO) {

            val filters = mutableListOf<Document>()

            // $in
            if (includeTags.isNotEmpty()) {
                filters += Document("tags", Document("\$in", includeTags))
            }

            // $nin
            if (excludeTags.isNotEmpty()) {
                filters += Document("tags", Document("\$nin", excludeTags))
            }

            // $or
            if (genresOr.isNotEmpty()) {
                filters += Document(
                    "\$or",
                    genresOr.map { Document("genre", it) }
                )
            }

            // $gt / $lt
            if (yearFrom != null || yearTo != null) {
                val yearCond = Document()
                yearFrom?.let { yearCond["\$gte"] = it }
                yearTo?.let { yearCond["\$lte"] = it }
                filters += Document("year", yearCond)
            }

            // $and (если условий > 1)
            val finalFilter =
                if (filters.size > 1) Document("\$and", filters)
                else filters.firstOrNull() ?: Document()

            DatabaseFactory.mongo
                .getCollection("books")
                .find(finalFilter)
                .projection(
                    Document("title", 1)
                        .append("year", 1)
                        .append("genre", 1)
                        .append("tags", 1)
                )
                .map { it.toApi() }
                .toList()
        }

    suspend fun deleteBooksBefore(year: Int): Long =
        withContext(Dispatchers.IO) {

            val result = DatabaseFactory.mongo
                .getCollection("books")
                .deleteMany(
                    Document("year", Document("\$lt", year))
                )

            result.deletedCount
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

    suspend fun getTopAuthors(limitCount: Int = 10): List<Map<String, Any?>> =
        withContext(Dispatchers.IO) {

            val authorsCol = DatabaseFactory.mongo.getCollection("authors")

            val pipeline = listOf(
                lookup(
                    "book_authors",
                    "_id",
                    "authorId",
                    "links"
                ),
                unwind("\$links"),

                lookup(
                    "books",
                    "links.bookId",
                    "_id",
                    "books"
                ),
                unwind("\$books"),

                group(
                    "\$_id",
                    first("name", "\$name"),
                    com.mongodb.client.model.Accumulators.sum("booksCount", 1)
                ),

                sort(Document("booksCount", -1)),
                limit(limitCount),

                com.mongodb.client.model.Aggregates.project(
                    Document("_id", 0)
                        .append("authorId", "\$_id")
                        .append("name", 1)
                        .append("booksCount", 1)
                )
            )

            authorsCol.aggregate(pipeline).map { it.toApi() }.toList()
        }

    suspend fun getAuthorsRatingAndGenres(): Map<String, Any?> =
        withContext(Dispatchers.IO) {

            val authorsCol = DatabaseFactory.mongo.getCollection("authors")

            val pipeline = listOf(
                lookup(
                    "book_authors",
                    "_id",
                    "authorId",
                    "links"
                ),
                unwind("\$links"),

                lookup(
                    "books",
                    "links.bookId",
                    "_id",
                    "books"
                ),
                unwind("\$books"),

                com.mongodb.client.model.Aggregates.facet(

                    // рейтинг
                    com.mongodb.client.model.Facet(
                        "authorsRating",
                        group(
                            "\$_id",
                            first("name", "\$name"),
                            com.mongodb.client.model.Accumulators.sum("booksCount", 1)
                        ),
                        sort(Document("booksCount", -1)),
                        limit(10),
                        com.mongodb.client.model.Aggregates.project(
                            Document("_id", 0)
                                .append("authorId", "\$_id")
                                .append("name", 1)
                                .append("booksCount", 1)
                        )
                    ),

                    // распределение
                    com.mongodb.client.model.Facet(
                        "genresDistribution",
                        unwind("\$books.genre"),
                        group(
                            "\$books.genre",
                            com.mongodb.client.model.Accumulators.sum("count", 1)
                        ),
                        sort(Document("count", -1)),
                        com.mongodb.client.model.Aggregates.project(
                            Document("_id", 0)
                                .append("genre", "\$_id")
                                .append("count", 1)
                        )
                    )
                )
            )

            authorsCol.aggregate(pipeline).firstOrNull()?.toApi()
                ?: emptyMap()
        }

    suspend fun getAvailabilityByOffice(): List<Map<String, Any?>> =
        withContext(Dispatchers.IO) {

            val copiesCol = DatabaseFactory.mongo.getCollection("book_copies")

            val pipeline = listOf(
                match(eq("status", "AVAILABLE")),

                lookup(
                    "offices",
                    "officeId",
                    "_id",
                    "office"
                ),
                unwind("\$office"),

                group(
                    "\$office._id",
                    first("officeName", "\$office.name"),
                    com.mongodb.client.model.Accumulators.sum("availableCopies", 1)
                ),

                sort(Document("availableCopies", -1)),

                com.mongodb.client.model.Aggregates.project(
                    Document("_id", 0)
                        .append("officeId", "\$_id")
                        .append("officeName", 1)
                        .append("availableCopies", 1)
                )
            )

            copiesCol.aggregate(pipeline).map { it.toApi() }.toList()
        }

    suspend fun materializeTopAuthorsCache() =
        withContext(Dispatchers.IO) {

            val authorsCol = DatabaseFactory.mongo.getCollection("authors")

            val pipeline = listOf(
                lookup(
                    "book_authors",
                    "_id",
                    "authorId",
                    "links"
                ),
                unwind("\$links"),

                lookup(
                    "books",
                    "links.bookId",
                    "_id",
                    "books"
                ),
                unwind("\$books"),

                group(
                    "\$_id",
                    first("name", "\$name"),
                    com.mongodb.client.model.Accumulators.sum("booksCount", 1)
                ),

                sort(Document("booksCount", -1)),

                com.mongodb.client.model.Aggregates.project(
                    Document("_id", 0)
                        .append("authorId", "\$_id")
                        .append("name", 1)
                        .append("booksCount", 1)
                ),

                com.mongodb.client.model.Aggregates.merge(
                    "top_authors_cache"
                )
            )

            authorsCol.aggregate(pipeline).toList()
        }

    suspend fun createBookAuthorTransactional(): Map<String, Any?> = withContext(Dispatchers.IO) {
        val client = DatabaseFactory.mongoClient
        val db = DatabaseFactory.mongo

        val authors = db.getCollection("authors")
        val books = db.getCollection("books")
        val links = db.getCollection("book_authors")

        client.startSession().use { session ->
            try {
                session.startTransaction()

                val authorDoc = Document("name", "Tx Author").append("bio", "created in tx")
                authors.insertOne(session, authorDoc)
                val authorId = authorDoc.getObjectId("_id")

                val bookDoc = Document("title", "Tx Book")
                    .append("genre", "tx")
                    .append("year", 2024)
                    .append("isbnNumber", "TX-${System.currentTimeMillis()}")
                    .append("tags", listOf("tx"))
                books.insertOne(session, bookDoc)
                val bookId = bookDoc.getObjectId("_id")

                links.insertOne(
                    session,
                    Document("bookId", bookId).append("authorId", authorId).append("role", "author")
                )

                session.commitTransaction()

                return@withContext mapOf(
                    "bookId" to bookId.toHexString(),
                    "authorId" to authorId.toHexString(),
                    "status" to "committed"
                )
            } catch (e: Exception) {
                session.abortTransaction()
                throw e
            }
        }
    }

    suspend fun bulkDemo(): Map<String, Any?> = withContext(Dispatchers.IO) {
        val col = DatabaseFactory.mongo.getCollection("books")

        val ops = listOf(
            com.mongodb.client.model.UpdateOneModel(
                Document("isbnNumber", "ISBN-978-1"),
                Document("\$inc", Document("views", 1))
            ),
            com.mongodb.client.model.UpdateManyModel(
                Document("year", Document("\$lt", 1990)),
                Document("\$addToSet", Document("tags", "bulk-old"))
            ),
            com.mongodb.client.model.ReplaceOneModel(
                Document("isbnNumber", "BULK-REPLACE"),
                Document("isbnNumber", "BULK-REPLACE").append("title", "Bulk Replaced").append("year", 2024),
                com.mongodb.client.model.ReplaceOptions().upsert(true)
            ),
            com.mongodb.client.model.DeleteOneModel(
                Document("isbnNumber", "BULK-DELETE")
            ),
            com.mongodb.client.model.InsertOneModel(
                Document("isbnNumber", "BULK-INSERT-${System.currentTimeMillis()}")
                    .append("title", "Bulk Insert")
                    .append("year", 2024)
            )
        )

        val res = col.bulkWrite(ops)

        mapOf(
            "inserted" to res.insertedCount,
            "matched" to res.matchedCount,
            "modified" to res.modifiedCount,
            "deleted" to res.deletedCount
        )
    }

    fun applyBooksValidation() {
        val db = DatabaseFactory.mongo

        val validator = Document("\$jsonSchema", Document()
            .append("bsonType", "object")
            .append("required", listOf("title", "isbnNumber", "year"))
            .append("properties", Document()
                .append("title", Document("bsonType", "string").append("minLength", 1))
                .append("isbnNumber", Document("bsonType", "string").append("minLength", 5))
                .append("year", Document("bsonType", "int").append("minimum", 1500).append("maximum", 2025))
                .append("tags", Document("bsonType", "array").append("maxItems", 20)
                    .append("items", Document("bsonType", "string")))
            )
        )

        db.runCommand(
            Document("collMod", "books")
                .append("validator", validator)
                .append("validationLevel", "moderate")
                .append("validationAction", "error")
        )
    }



}