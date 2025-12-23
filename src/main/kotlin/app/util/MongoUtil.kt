package com.example.app.util

import org.bson.Document
import org.bson.types.ObjectId

fun Document.toApi(): Map<String, Any?> =
    this.toMutableMap().apply {
        this["_id"] = (this["_id"] as ObjectId).toHexString()
    }
