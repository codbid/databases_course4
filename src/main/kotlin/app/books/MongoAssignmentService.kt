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

    fun createIndexes() {
        val mongo = DatabaseFactory.mongo

        val books = mongo.getCollection("books")
        val authors = mongo.getCollection("authors")
        val links = mongo.getCollection("book_authors")

        books.createIndex(Indexes.ascending("isbnNumber"), IndexOptions().unique(true))
        books.createIndex(Indexes.text("title"))
        books.createIndex(Indexes.ascending("tags"))
        books.createIndex(Indexes.ascending("tags", "year"))
        books.createIndex(Indexes.ascending("year"), IndexOptions().partialFilterExpression(
                Document("year", Document("\$gt", 2020))
            )
        )
        authors.createIndex(Indexes.ascending("name"), IndexOptions().unique(true))
        links.createIndex(Indexes.ascending("bookId", "authorId"), IndexOptions().unique(true))
        links.createIndex(Indexes.ascending("bookId"))
        links.createIndex(Indexes.ascending("authorId"))
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