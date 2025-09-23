package com.example.app.users.clients.DAO

import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass

object ClientsTable : LongIdTable("clients") {

    val name = varchar("name", 255)
    val email = varchar("email", 255)
    val password = varchar("password", 255)
    val city = varchar("city", 255)
    val createdAt = datetime("created_at")

}

class ClientEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ClientEntity>(ClientsTable)

    var name by ClientsTable.name
    var email by ClientsTable.email
    var password by ClientsTable.password // TODO: Да, не безопасно, каюсь, но пока затычка))
    var city by ClientsTable.city
    var createdAt by ClientsTable.createdAt

}
