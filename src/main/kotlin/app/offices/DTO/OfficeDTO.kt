package com.example.app.offices.DTO

import com.example.app.offices.DAO.OfficeEntity

data class OfficeResponse(val office: OfficeEntity) {
    val id: Long = office.id.value
    val name: String = office.name
    val address: String = office.address
    val workingTime: String = office.workingTime
}

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