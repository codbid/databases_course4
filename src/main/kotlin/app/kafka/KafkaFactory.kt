package com.example.app.kafka

import org.apache.kafka.clients.producer.KafkaProducer

object KafkaFactory {
    val producer: KafkaProducer<String, String>
        get() = com.example.config.KafkaFactory.producer
}
