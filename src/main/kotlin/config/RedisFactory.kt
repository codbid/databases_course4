package com.example.config

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.config.ApplicationConfig
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object RedisFactory {

    lateinit var pool: JedisPool
        private set

    fun init(app: Application, config: ApplicationConfig) {
        val host = config.property("redis.host").getString()
        val port = config.property("redis.port").getString().toInt()

        pool = JedisPool(JedisPoolConfig(), host, port)

        app.environment.monitor.subscribe(ApplicationStopped) {
            pool.close()
        }
    }
}
