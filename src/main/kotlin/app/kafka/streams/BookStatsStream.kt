package com.example.app.kafka.streams

import com.example.app.kafka.KafkaTopics
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.TimeWindows
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.util.Properties

object BookStatsStream {

    fun start() {

        val props = Properties()
        props["application.id"] = "book-stats-app-" + System.currentTimeMillis()
        props["bootstrap.servers"] = "localhost:9092"
        props["auto.offset.reset"] = "earliest"

        props["default.key.serde"] = Serdes.String().javaClass
        props["default.value.serde"] = Serdes.String().javaClass

        val builder = StreamsBuilder()

        val stream = builder.stream<String, String>(KafkaTopics.BOOK_EVENTS)

        val stats = stream

            .filter { _, v ->
                JSONObject(v).getString("eventType") == "BookLoaned"
            }

            .selectKey { _, v ->
                val json = JSONObject(v)
                val payload = json.getJSONObject("payload")

                val bookId = payload.getLong("bookCopyId")
                val ts = json.getString("timestamp").substring(0, 13) + ":00:00"

                "$bookId|$ts"
            }

            .groupByKey()
            .count()

        stats.toStream().map { key, value ->

            val parts = key.split("|")
            val bookId = parts[0].toLong()
            val hour = parts[1]

            val schema = JSONObject()
            val fields = JSONArray()

            fields.put(JSONObject().put("type", "int64").put("field", "book_id"))
            fields.put(JSONObject().put("type", "string").put("field", "hour"))
            fields.put(JSONObject().put("type", "int64").put("field", "issued_count"))

            schema.put("type", "struct")
            schema.put("fields", fields)

            val payload = JSONObject()
            payload.put("book_id", bookId)
            payload.put("hour", hour)
            payload.put("issued_count", value)

            val result = JSONObject()
            result.put("schema", schema)
            result.put("payload", payload)

            println("OUT: $result")

            KeyValue(key, result.toString())

        }.to(KafkaTopics.BOOK_STATS, Produced.with(Serdes.String(), Serdes.String()))

        val streams = KafkaStreams(builder.build(), props)
        streams.start()

        println("STREAM STARTED")
    }
}