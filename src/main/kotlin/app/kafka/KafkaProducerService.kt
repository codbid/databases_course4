package com.example.app.kafka

import com.example.config.KafkaFactory
import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.apache.kafka.clients.producer.ProducerRecord

class KafkaProducerService(private val application: Application) {

    private val producer = KafkaFactory.producer
    private val topic = KafkaFactory.config.topic

    fun sendMessage(key: String?, value: String) {
        val record = ProducerRecord(topic, key, value)
        producer.send(record) { metadata, exception ->
            if (exception != null) {
                application.log.error("Failed to send Kafka message", exception)
            } else if (metadata != null) {
                application.log.info("Sent Kafka message to ${metadata.topic()}[${metadata.partition()}]@${metadata.offset()}")
            }
        }
    }
}

