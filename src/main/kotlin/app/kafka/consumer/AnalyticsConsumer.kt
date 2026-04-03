package com.example.app.kafka.consumer

import com.example.app.kafka.KafkaTopics
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.Properties

object AnalyticsConsumer {

    fun start() {
        val props = Properties()

        props["bootstrap.servers"] = "localhost:9092"
        props["group.id"] = "analytics-group"
        props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"

        props["enable.auto.commit"] = "true"

        val consumer = KafkaConsumer<String, String>(props)
        consumer.subscribe(listOf(KafkaTopics.BOOK_EVENTS))

        while (true) {
            val records = consumer.poll(Duration.ofMillis(500))

            for (record in records) {
                println("Analytics event: ${record.value()}")
            }
        }
    }
}