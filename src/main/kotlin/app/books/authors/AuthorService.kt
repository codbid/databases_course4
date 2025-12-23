package com.example.app.books.authors

import com.example.app.util.toApi
import com.example.config.DatabaseFactory
import org.bson.Document

object AuthorService {

    fun createAuthor(name: String): Map<String, Any?> {
        val doc = Document("name", name)
        DatabaseFactory.mongo.getCollection("authors").insertOne(doc)
        return doc.toApi()
    }

    fun getAuthors(): List<Map<String, Any?>> =
        DatabaseFactory.mongo.getCollection("authors").find().map { it.toApi() }.toList()
}