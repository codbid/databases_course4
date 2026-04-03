package com.example.app.kafka

object KafkaTopics {
    const val BOOK_EVENTS = "library.book-events"
    const val BOOK_EVENTS_DLQ = "library.book-events.dlq"
    const val BOOK_STATS = "library.book-stats-hourly"
}