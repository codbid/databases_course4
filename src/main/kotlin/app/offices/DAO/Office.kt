package com.example.app.offices.DAO

import com.example.app.books.DAO.BookCopiesTable
import com.example.app.books.DAO.BookCopyEntity
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable

object OfficesTable : LongIdTable("offices") {
    val name = varchar("name", 255)
    val address = varchar("address", 255)
    val workingTime = varchar("working_time", 255)
}

class OfficeEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<OfficeEntity>(OfficesTable)
    var name by OfficesTable.name
    var address by OfficesTable.address
    var workingTime by OfficesTable.workingTime
}