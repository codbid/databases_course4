package com.example.config

import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.config.tryGetString
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

object KafkaFactory {

    data class KafkaConfig(
        val bootstrapServers: String,
        val topic: String,
        val groupId: String
    )

    private var _config: KafkaConfig? = null
    val config: KafkaConfig get() = _config!!

    private var _producer: KafkaProducer<String, String>? = null
    val producer: KafkaProducer<String, String> get() = _producer!!

    var isInitialized: Boolean = false
        private set

    fun init(application: Application) {
        val cfg = application.environment.config
        val bootstrap = cfg.tryGetString("kafka.bootstrapServers") ?: return
        val topic = cfg.tryGetString("kafka.topic") ?: return
        val groupId = cfg.tryGetString("kafka.groupId") ?: return

        _config = KafkaConfig(
            bootstrapServers = bootstrap,
            topic = topic,
            groupId = groupId
        )

        val producerProps = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.ACKS_CONFIG, "all")
        }

        _producer = KafkaProducer(producerProps)
        isInitialized = true

        application.log.info("Kafka producer initialized with bootstrapServers=$bootstrap, topic=$topic, groupId=$groupId")
    }

    fun createConsumer(): KafkaConsumer<String, String> {
        val c = _config ?: error("KafkaFactory not initialized")
        val consumerProps = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, c.bootstrapServers)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.GROUP_ID_CONFIG, c.groupId)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
        }
        return KafkaConsumer(consumerProps)
    }
}

