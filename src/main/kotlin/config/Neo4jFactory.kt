package com.example.config

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase

object Neo4jFactory {

    private lateinit var driver: Driver

    fun init(config: ApplicationConfig, app: Application) {
        val uri = config.tryGetString("db.neo4j.uri") ?: "bolt://localhost:7687"
        val user = config.tryGetString("db.neo4j.user") ?: "neo4j"
        val password = config.tryGetString("db.neo4j.password") ?: "password"
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password))
        app.environment.monitor.subscribe(ApplicationStopped) {
            driver.close()
        }
    }

    fun driver(): Driver = driver
}
