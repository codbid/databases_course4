package com.example.app.kafka.consumer

import com.example.app.kafka.KafkaFactory
import com.example.app.kafka.KafkaTopics
import org.apache.kafka.clients.producer.ProducerRecord
import org.joda.time.Instant
import org.json.JSONObject

object DLQProducer {

    fun send(key: String, originalEvent: String, error: String) {

        val dlqMessage = JSONObject(
            mapOf(
                "failedAt" to Instant.now().toString(),
                "error" to error,
                "originalEvent" to JSONObject(originalEvent)
            )
        ).toString()

        KafkaFactory.producer.send(
            ProducerRecord(
                KafkaTopics.BOOK_EVENTS_DLQ,
                key,
                dlqMessage
            )
        )
    }
}