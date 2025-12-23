package com.example.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString
import org.bson.Document
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import java.util.concurrent.TimeUnit

object DatabaseFactory {

    private lateinit var appConfig: ApplicationConfig
    lateinit var mongoClient: MongoClient
    lateinit var mongo: MongoDatabase
        private set

    private val dbPostgresDriver: String by lazy { appConfig.property("db.postgres.jdbcDriver").getString() }
    private val dbPostgresUrl: String by lazy { appConfig.property("db.postgres.url").getString() }
    private val dbPostgresUser: String by lazy { appConfig.property("db.postgres.user").getString() }
    private val dbPostgresPassword: String by lazy { appConfig.property("db.postgres.password").getString() }

    fun init(app: Application, config: ApplicationConfig) {
        appConfig = config

        Database.connect(hikari())

        val flyway = Flyway.configure()
            .dataSource(dbPostgresUrl, dbPostgresUser, dbPostgresPassword)
            .locations("classpath:db/migrations")
            .baselineOnMigrate(true)
            .outOfOrder(true)
            .load()

        if (appConfig.property("flyway.clean").getString().toBoolean()) {
            flyway.clean()
        }

        flyway.migrate()

        val user = config.tryGetString("db.mongo.user")
        val password = config.tryGetString("db.mongo.password")
        val host = config.tryGetString("db.mongo.host") ?: "127.0.0.1"
        val port = config.tryGetString("db.mongo.port") ?: "27017"
        val maxPoolSize = config.tryGetString("db.mongo.maxPoolSize")?.toInt() ?: 20
        val databaseName = config.tryGetString("db.mongo.database.name") ?: "myDatabase"

        val credentials = user?.let { userVal -> password?.let { passwordVal -> "$userVal:$passwordVal@" } }.orEmpty()
        val uri = "mongodb://$credentials$host:$port/?maxPoolSize=$maxPoolSize&w=majority"

        mongoClient = MongoClients.create(uri)
        mongo = mongoClient.getDatabase(databaseName)
        mongo

        app.environment.monitor.subscribe(ApplicationStopped) {
            mongoClient.close()
        }

        mongoInit()
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = dbPostgresDriver
        config.jdbcUrl = dbPostgresUrl
        config.username = dbPostgresUser
        config.password = dbPostgresPassword
        config.maximumPoolSize = appConfig.property("db.postgres.maxPoolSize").getString().toInt()
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    private fun mongoInit() {
        try {
            mongo.createCollection("authors")//.createIndex(Indexes.ascending("name"), IndexOptions().unique(true))
            mongo.createCollection("books")//.createIndex(Indexes.text("title"))
            mongo.createCollection("books")//.createIndex(Indexes.ascending("tags"))
            mongo.createCollection("book_authors")//.createIndex(Indexes.ascending("bookId", "authorId"))
            //val partialOptions = IndexOptions().partialFilterExpression(Document("year", Document("\$gt", 2020)))
            mongo.createCollection("books")//.createIndex(Indexes.ascending("year"), partialOptions)
            mongo.createCollection("system_codes")//.createIndex(Indexes.ascending("createdAt"), IndexOptions().expireAfter(3600L, TimeUnit.SECONDS))
        } catch (ignore: Exception) {}
    }
}