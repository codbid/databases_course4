package com.example.app.kafka.consumer

import com.example.app.books.BookCopyStatus
import com.example.app.books.DAO.BookCopyEntity
import com.example.app.kafka.KafkaTopics
import com.example.app.operations.DAO.LoanEntity
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONObject
import java.time.Duration
import java.util.Properties

object InventoryConsumer {

    fun start() {
        val props = Properties()

        props["bootstrap.servers"] = "localhost:9092"
        props["group.id"] = "inventory-group"
        props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"

        props["enable.auto.commit"] = "false"

        val consumer = KafkaConsumer<String, String>(props)
        consumer.subscribe(listOf(KafkaTopics.BOOK_EVENTS))

        while (true) {
            val records = consumer.poll(Duration.ofMillis(500))

            for (record in records) {
                try {
                    process(record.value())

                    consumer.commitSync()

                } catch (e: Exception) {
                    DLQProducer.send(
                        record.key(),
                        record.value(),
                        e.message ?: "unknown error"
                    )
                }
            }
        }
    }

    private fun process(eventJson: String) {

        val event = JSONObject(eventJson)
        val type = event.getString("eventType")
        val payload = event.getJSONObject("payload")

        when (type) {

            "BookIssued" -> {
                val bookCopyId = payload.getInt("bookCopyId")

                transaction {
                    val entity = BookCopyEntity.findById(bookCopyId.toLong())
                        ?: return@transaction

                    entity.status = BookCopyStatus.AVAILABLE
                }
            }

            "BookLoaned" -> {
                val bookCopyId = payload.getInt("bookCopyId")

                transaction {
                    val entity = BookCopyEntity.findById(bookCopyId.toLong())
                        ?: return@transaction

                    entity.status = BookCopyStatus.UNAVAILABLE
                }
            }

            "BookReturned" -> {
                val loanId = payload.getInt("loanId")

                transaction {
                    val loan = LoanEntity.findById(loanId.toLong())
                        ?: return@transaction

                    loan.bookCopy.status = BookCopyStatus.AVAILABLE
                }
            }
        }
    }
}