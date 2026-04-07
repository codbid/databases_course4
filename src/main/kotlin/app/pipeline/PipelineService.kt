package com.example.app.pipeline

import com.example.app.kafka.EventBuilder
import com.example.app.kafka.KafkaTopics
import com.example.config.DatabaseFactory
import com.example.config.KafkaFactory
import io.ktor.server.application.Application
import io.ktor.server.config.tryGetString
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Properties
import java.util.UUID

object PipelineService {
    private lateinit var application: Application
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build()

    fun init(application: Application) {
        this.application = application
    }

    fun connectorName(): String = "book-stats-jdbc-sink"

    fun uiTopics(): List<String> = listOf(
        KafkaTopics.BOOK_EVENTS,
        KafkaTopics.BOOK_STATS,
        KafkaTopics.BOOK_EVENTS_DLQ,
    )

    private fun pipelineConfig(path: String): String =
        application.environment.config.tryGetString(path)
            ?: error("Missing config property: $path")

    private fun bootstrapServers(): String = KafkaFactory.config.bootstrapServers

    private fun connectUrl(): String = pipelineConfig("pipeline.kafkaConnectUrl").trimEnd('/')

    private fun clickHouseUrl(): String = pipelineConfig("pipeline.clickhouseUrl")

    private fun clickHouseAuthHeader(): String {
        val user = pipelineConfig("pipeline.clickhouseUser")
        val password = pipelineConfig("pipeline.clickhousePassword")
        val raw = "$user:$password"
        return java.util.Base64.getEncoder().encodeToString(raw.toByteArray())
    }

    fun ensureTopics() {
        if (!KafkaFactory.isInitialized) return
        val adminProps = Properties().apply {
            put("bootstrap.servers", bootstrapServers())
        }
        AdminClient.create(adminProps).use { admin ->
            val existing = admin.listTopics().names().get()
            val required = listOf(
                NewTopic(KafkaTopics.BOOK_EVENTS, 3, 1),
                NewTopic(KafkaTopics.BOOK_STATS, 3, 1),
                NewTopic(KafkaTopics.BOOK_EVENTS_DLQ, 3, 1),
            ).filterNot { it.name() in existing }

            if (required.isNotEmpty()) {
                admin.createTopics(required).all().get()
            }
        }
    }

    fun topicSnapshot(topic: String): TopicSnapshot {
        if (!KafkaFactory.isInitialized) {
            return TopicSnapshot(topic, 0, 0, emptyMap())
        }
        val adminProps = Properties().apply {
            put("bootstrap.servers", bootstrapServers())
        }
        AdminClient.create(adminProps).use { admin ->
            val topicDescription = admin.describeTopics(listOf(topic)).allTopicNames().get()[topic]
                ?: return TopicSnapshot(topic, 0, 0, emptyMap())
            val partitions = topicDescription.partitions().map { TopicPartition(topic, it.partition()) }
            if (partitions.isEmpty()) {
                return TopicSnapshot(topic, 0, 0, emptyMap())
            }

            val consumer = ephemeralConsumer("snapshot-$topic")
            consumer.use {
                val endOffsets = it.endOffsets(partitions)
                val latestOffsets = partitions.associate { partition ->
                    partition.partition() to (endOffsets[partition] ?: 0L)
                }
                return TopicSnapshot(
                    topic = topic,
                    partitions = partitions.size,
                    totalMessages = latestOffsets.values.sum(),
                    latestOffsets = latestOffsets,
                )
            }
        }
    }

    fun recentMessages(topic: String, limit: Int = 20): List<Map<String, Any?>> {
        if (!KafkaFactory.isInitialized) {
            return emptyList()
        }
        val consumer = ephemeralConsumer("inspect-$topic-${UUID.randomUUID()}")
        consumer.use {
            val partitions = it.partitionsFor(topic).map { info -> TopicPartition(topic, info.partition()) }
            if (partitions.isEmpty()) {
                return emptyList()
            }

            it.assign(partitions)
            val endOffsets = it.endOffsets(partitions)
            partitions.forEach { partition ->
                val endOffset = endOffsets[partition] ?: 0L
                val startOffset = (endOffset - limit).coerceAtLeast(0L)
                it.seek(partition, startOffset)
            }

            val records = mutableListOf<Map<String, Any?>>()
            val deadline = System.currentTimeMillis() + 1500
            while (System.currentTimeMillis() < deadline && records.size < limit) {
                val polled = it.poll(Duration.ofMillis(250))
                for (record in polled) {
                    records += mapOf(
                        "topic" to record.topic(),
                        "partition" to record.partition(),
                        "offset" to record.offset(),
                        "key" to record.key(),
                        "value" to record.value(),
                        "timestamp" to record.timestamp(),
                    )
                }
            }
            return records.sortedByDescending { (it["offset"] as? Long) ?: -1L }.take(limit)
        }
    }

    fun sendManualEvent(request: ManualEventRequest): Map<String, Any> {
        check(KafkaFactory.isInitialized) { "Kafka is not initialized" }
        val entityId = request.entityId?.takeIf { it.isNotBlank() }
            ?: "${request.eventType.lowercase()}-${UUID.randomUUID()}"

        val payload = mutableMapOf<String, Any>(
            "officeId" to request.officeId,
            "bookId" to request.bookId,
            "bookCopyId" to request.bookCopyId,
            "status" to request.status,
        )
        request.clientId?.let { payload["clientId"] = it }
        when (request.eventType) {
            "BookLoaned" -> payload["loanId"] = System.currentTimeMillis()
            "ReservationCreated" -> payload["reservationId"] = System.currentTimeMillis()
        }

        val event = EventBuilder.build(
            eventType = request.eventType,
            entityId = entityId,
            payload = payload,
        )

        KafkaFactory.producer.send(
            ProducerRecord(KafkaTopics.BOOK_EVENTS, request.bookCopyId.toString(), event),
        ).get()

        return mapOf("status" to "queued", "topic" to KafkaTopics.BOOK_EVENTS, "payload" to event)
    }

    fun queryClickHouse(sql: String): List<Map<String, String>> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(clickHouseUrl()))
            .header("Authorization", "Basic ${clickHouseAuthHeader()}")
            .header("Content-Type", "text/plain")
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString("$sql FORMAT TabSeparatedWithNames"))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("ClickHouse query failed: ${response.statusCode()} ${response.body()}")
        }

        val lines = response.body().lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) {
            return emptyList()
        }
        val headers = lines.first().split('\t')
        return lines.drop(1).map { line ->
            val values = line.split('\t')
            headers.mapIndexed { index, header -> header to values.getOrElse(index) { "" } }.toMap()
        }
    }

    fun clickHouseOverview(): Map<String, String> {
        val rows = queryClickHouse(
            """
            SELECT 'book_events' AS metric, toString(count()) AS value FROM library_analytics.book_events
            UNION ALL
            SELECT 'daily_office_metrics', toString(count()) FROM library_analytics.daily_office_metrics
            UNION ALL
            SELECT 'unique_event_ids', toString(uniqExact(event_id)) FROM library_analytics.book_events
            UNION ALL
            SELECT 'stream_topic_rows', toString(count()) FROM library_analytics.book_events WHERE source IN ('library-backend', 'synthetic-load')
            """.trimIndent(),
        )
        return rows.associate { row -> row.getValue("metric") to row.getValue("value") }
    }

    fun postgresSinkSnapshot(): Map<String, Any?> = transaction {
        val exists = execAndCollect(
            """
            SELECT EXISTS (
              SELECT 1
              FROM information_schema.tables
              WHERE table_schema = 'public' AND table_name = 'book_stats_hourly'
            ) AS exists
            """.trimIndent(),
        ).firstOrNull()?.get("exists")?.toBooleanStrictOrNull() ?: false

        if (!exists) {
            return@transaction mapOf("tablePresent" to false, "rowCount" to 0, "sample" to emptyList<Map<String, String>>())
        }

        val rowCount = execAndCollect("SELECT count(*) AS row_count FROM public.book_stats_hourly")
            .firstOrNull()?.get("row_count")?.toLongOrNull() ?: 0L
        val sample = execAndCollect(
            """
            SELECT office_id, event_type, window_start, window_end, events_count
            FROM public.book_stats_hourly
            ORDER BY window_start DESC
            LIMIT 10
            """.trimIndent(),
        )
        mapOf("tablePresent" to true, "rowCount" to rowCount, "sample" to sample)
    }

    private fun execAndCollect(sql: String): List<Map<String, String>> {
        val rows = mutableListOf<Map<String, String>>()
        org.jetbrains.exposed.sql.transactions.TransactionManager.current().exec(sql) { rs ->
            val meta = rs.metaData
            while (rs.next()) {
                val row = buildMap {
                    for (index in 1..meta.columnCount) {
                        put(meta.getColumnLabel(index), rs.getString(index) ?: "")
                    }
                }
                rows += row
            }
        }
        return rows
    }

    fun connectorStatus(): Any? {
        return try {
            val response = httpGet("${connectUrl()}/connectors/${connectorName()}/status")
            response
        } catch (_: Exception) {
            mapOf("status" to "unavailable")
        }
    }

    fun registerConnector(): ConnectorRegistrationResponse {
        val configText = application::class.java.getResource("/kafka-connect/book-stats-jdbc-sink.json")
            ?.readText()
            ?: error("Connector config resource not found")

        val body = httpPut("${connectUrl()}/connectors/${connectorName()}/config", configText)
        return ConnectorRegistrationResponse("ok", connectorName(), body)
    }

    fun pipelineOverview(): PipelineOverview {
        val kafkaSnapshots = uiTopics().map { topicSnapshot(it) }
        return PipelineOverview(
            kafka = kafkaSnapshots,
            clickHouse = try {
                clickHouseOverview()
            } catch (error: Exception) {
                mapOf("error" to (error.message ?: "clickhouse unavailable"))
            },
            postgresSink = try {
                postgresSinkSnapshot()
            } catch (error: Exception) {
                mapOf("error" to (error.message ?: "postgres unavailable"))
            },
            connectorStatus = connectorStatus(),
        )
    }

    private fun ephemeralConsumer(groupId: String): KafkaConsumer<String, String> {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers())
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
        }
        return KafkaConsumer(props)
    }

    private fun httpGet(url: String): Any {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(4))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return mapOf(
            "statusCode" to response.statusCode(),
            "body" to response.body(),
        )
    }

    private fun httpPut(url: String, body: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(6))
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Kafka Connect request failed: ${response.statusCode()} ${response.body()}")
        }
        return response.body()
    }
}
