package com.example.app.books

import com.example.config.DatabaseFactory
import com.mongodb.client.model.*
import org.bson.Document
import org.bson.types.ObjectId

object MongoAssignmentService {

    private val booksCol = DatabaseFactory.mongo.getCollection("books")
    private val authorsCol = DatabaseFactory.mongo.getCollection("authors")
    private val linkCol = DatabaseFactory.mongo.getCollection("book_authors")

    suspend fun clearAll() {
        booksCol.drop()
        authorsCol.drop()
        linkCol.drop()
    }

    suspend fun createIndexes() {
        // Unique
        booksCol.createIndex(Indexes.ascending("isbnNumber"), IndexOptions().unique(true))
        // Text
        booksCol.createIndex(Indexes.compoundIndex(Indexes.text("title"), Indexes.text("description")))
        // Compound
        linkCol.createIndex(Indexes.ascending("bookId", "authorId"))
        // Array
        booksCol.createIndex(Indexes.ascending("tags"))
        // Partial
        val partialOptions = IndexOptions().partialFilterExpression(Document("year", Document("\$gt", 2010)))
        booksCol.createIndex(Indexes.ascending("year"), partialOptions)
    }

    suspend fun seedData() {
        val authorDocs = (1..80).map { i ->
            Document("_id", ObjectId()).append("name", "Author $i").append("bio", "Bio $i")
        }
        authorsCol.insertMany(authorDocs)

        val bookDocs = (1..400).map { i ->
            Document("_id", ObjectId())
                .append("title", "Book $i")
                .append("isbnNumber", "ISBN-978-$i")
                .append("year", (1980..2024).random())
                .append("description", "Description for book $i")
                .append("tags", listOf("classic", "science").shuffled())
        }
        booksCol.insertMany(bookDocs)

        val links = (1..600).map {
            Document("bookId", bookDocs.random().getObjectId("_id"))
                .append("authorId", authorDocs.random().getObjectId("_id"))
        }
        linkCol.insertMany(links)
    }

    suspend fun runPerformanceTest(): String {
        val filter = Document("isbnNumber", "ISBN-978-350")

        val startTime = System.nanoTime()
        val explain = booksCol.find(filter).explain()
        val endTime = System.nanoTime()

        val duration = (endTime - startTime) / 1_000_000.0
        val stage = explain.get("queryPlanner", Document::class.java)
            .get("winningPlan", Document::class.java)
            .get("stage")

        return "Результат: Стадия = $stage, Время = $duration мс"
    }
}