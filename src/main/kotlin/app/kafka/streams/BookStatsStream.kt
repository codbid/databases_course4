package com.example.app.kafka.streams

import com.example.app.kafka.KafkaTopics
import com.example.config.KafkaFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.TimeWindows
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.util.Properties

object BookStatsStream {
    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true

        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "library-book-stats-v1"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = KafkaFactory.config.bootstrapServers
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.StringSerde::class.java
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.StringSerde::class.java
        props[StreamsConfig.STATE_DIR_CONFIG] = System.getProperty("java.io.tmpdir") + "/library-book-stats"
        props[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = 1_000
        props["auto.offset.reset"] = "earliest"

        val builder = StreamsBuilder()

        val stats = builder.stream<String, String>(KafkaTopics.BOOK_EVENTS)
            .filter { _, value ->
                try {
                    val json = JSONObject(value)
                    json.has("eventType") && json.has("payload")
                } catch (_: Exception) {
                    false
                }
            }
            .selectKey { _, value ->
                val json = JSONObject(value)
                val payload = json.getJSONObject("payload")
                val officeId = payload.optLong("officeId", 0L)
                val eventType = json.optString("eventType", "Unknown")
                "$officeId|$eventType"
            }
            .groupByKey()
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
            .count()

        stats.toStream().map { key, value ->
            val parts = key.key().split("|")
            val officeId = parts.getOrElse(0) { "0" }.toLong()
            val eventType = parts.getOrElse(1) { "Unknown" }
            val windowStart = java.time.Instant.ofEpochMilli(key.window().start()).toString()
            val windowEnd = java.time.Instant.ofEpochMilli(key.window().end()).toString()

            val schema = JSONObject()
            val fields = JSONArray()

            fields.put(JSONObject().put("type", "int64").put("field", "office_id"))
            fields.put(JSONObject().put("type", "string").put("field", "event_type"))
            fields.put(JSONObject().put("type", "string").put("field", "window_start"))
            fields.put(JSONObject().put("type", "string").put("field", "window_end"))
            fields.put(JSONObject().put("type", "int64").put("field", "events_count"))

            schema.put("type", "struct")
            schema.put("fields", fields)
            schema.put("optional", false)

            val payload = JSONObject()
            payload.put("office_id", officeId)
            payload.put("event_type", eventType)
            payload.put("window_start", windowStart)
            payload.put("window_end", windowEnd)
            payload.put("events_count", value)

            val result = JSONObject()
            result.put("schema", schema)
            result.put("payload", payload)

            KeyValue("${officeId}|${eventType}|${windowStart}", result.toString())
        }.to(KafkaTopics.BOOK_STATS, Produced.with(Serdes.String(), Serdes.String()))

        val streams = KafkaStreams(builder.build(), props)
        streams.setUncaughtExceptionHandler {
            it.printStackTrace()
            org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD
        }
        streams.start()

        Runtime.getRuntime().addShutdownHook(Thread { streams.close(Duration.ofSeconds(5)) })
        println("BOOK STATS STREAM STARTED")
    }
}
