package com.example.app.operations.DTO

import com.example.app.operations.DAO.ReservationEntity

data class ReservationCreateRequest(
    val bookCopyID: Long,
    val clientID: Long,
    val durationInDays: Int
)

data class ReservationResponse(
    val id: Long,
    val clientID: Long,
    val bookCopyID: Long,
    val startDate: String,
    val endDate: String
)

fun ReservationEntity.toResponse(): ReservationResponse = ReservationResponse(
    id = id.value,
    clientID = client.id.value,
    bookCopyID = bookCopy.id.value,
    startDate = startDate.toString(),
    endDate = endDate.toString()
)