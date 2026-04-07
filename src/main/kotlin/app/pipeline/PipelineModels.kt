package com.example.app.pipeline

data class TopicSnapshot(
    val topic: String,
    val partitions: Int,
    val totalMessages: Long,
    val latestOffsets: Map<Int, Long>,
)

data class PipelineOverview(
    val kafka: List<TopicSnapshot>,
    val clickHouse: Map<String, String>,
    val postgresSink: Map<String, Any?>,
    val connectorStatus: Any?,
)

data class ConnectorRegistrationResponse(
    val status: String,
    val connectorName: String,
    val responseBody: String,
)

data class ManualEventRequest(
    val eventType: String,
    val officeId: Long,
    val clientId: Long? = null,
    val bookId: Long,
    val bookCopyId: Long,
    val entityId: String? = null,
    val status: String = "ACTIVE",
)

data class QueryRequest(
    val sql: String,
)
