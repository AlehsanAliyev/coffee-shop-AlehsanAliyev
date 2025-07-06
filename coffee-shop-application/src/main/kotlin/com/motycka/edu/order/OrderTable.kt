package com.motycka.edu.order

import com.motycka.edu.order.OrderStatus
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object OrderTable : LongIdTable("orders") {
    val customerId = long("customer_id")
    val status = enumerationByName("status", 20, OrderStatus::class)
}

class OrderDAO(id: EntityID<Long>) : LongEntity(id) {
    var customerId by OrderTable.customerId
    var status by OrderTable.status

    companion object : LongEntityClass<OrderDAO>(OrderTable)

    fun toDTO(): OrderDTO = OrderDTO(
        id = id.value,
        customerId = customerId,
        status = status
    )
}

