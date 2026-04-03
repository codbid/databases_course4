package com.example.app.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import java.util.Properties

object KafkaFactory {

    val producer: KafkaProducer<String, String> by lazy {
        val props = Properties()

        props["bootstrap.servers"] = "localhost:9092"
        props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"

        props["acks"] = "all"
        props["retries"] = 3

        KafkaProducer(props)
    }
}