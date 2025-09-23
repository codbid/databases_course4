package com.example.app.offices.DTO

import com.example.app.offices.DAO.OfficeEntity

data class OfficeResponse(
    val id: Long,
    val name: String,
    val address: String,
    val workingTime: String,
)

fun OfficeEntity.toResponse() = OfficeResponse(
    id = id.value,
    name = name,
    address = address,
    workingTime = workingTime
)

data class OfficeCreateRequest(
    val name: String,
    val address: String,
    val workingTime: String
)

data class OfficeUpdateRequest(
    val name: String? = null,
    val address: String? = null,
    val workingTime: String? = null
)