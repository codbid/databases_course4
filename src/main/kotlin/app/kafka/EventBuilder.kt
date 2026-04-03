package com.example.app.kafka

import org.joda.time.Instant
import org.json.JSONObject
import java.util.UUID

object EventBuilder {

    fun build(
        eventType: String,
        entityId: String,
        payload: Map<String, Any>
    ): String {
        return JSONObject(
            mapOf(
                "eventId" to UUID.randomUUID().toString(),
                "eventType" to eventType,
                "entityId" to entityId,
                "timestamp" to Instant.now().toString(),
                "source" to "library-backend",
                "version" to 1,
                "payload" to payload
            )
        ).toString()
    }
}