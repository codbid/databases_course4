package com.example.app.operations.DTO

import com.example.app.operations.DAO.ReservationEntity

data class ReservationCreateRequest(
    val bookCopyID: Long,
    val clientID: Long,
    val durationInDays: Int
)

data class ReservationResponse(val entity: ReservationEntity) {
    val id = entity.id
    val bookCopyID = entity.bookCopy.id.value
    val clientID = entity.client.id.value
    val startDate = entity.startDate
    val endDate = entity.endDate
}