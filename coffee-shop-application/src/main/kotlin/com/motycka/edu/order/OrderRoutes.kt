package com.motycka.edu.order

import com.motycka.edu.security.getUserIdentity
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val logger = KotlinLogging.logger {}

private const val ORDER_NOT_FOUND = "Order not found"
private const val INVALID_ID = "Invalid ID format"

fun Route.orderRoutes(
    orderService: OrderService,
    basePath: String
) {
    route("$basePath/orders") {

        get {
            logger.info { "GET /orders" }
            val orders = orderService.getAllOrders()
            call.respond(orders)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                logger.warn { "Invalid order ID" }
                call.respond(HttpStatusCode.BadRequest, INVALID_ID)
                return@get
            }

            val order = orderService.getOrderById(id)
            if (order != null) {
                call.respond(order)
            } else {
                logger.warn { "Order not found: $id" }
                call.respond(HttpStatusCode.NotFound, ORDER_NOT_FOUND)
            }
        }

        post {
            val identity = getUserIdentity()
            val request = call.receive<OrderRequest>()

            logger.info { "POST /orders by customerId=${identity.customerId}" }

            try {
                val createdOrder = orderService.createOrder(identity.customerId, request)
                call.respond(HttpStatusCode.Created, createdOrder)
            } catch (e: Exception) {
                logger.error(e) { "Error creating order" }
                call.respond(HttpStatusCode.InternalServerError, "Failed to create order")
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, INVALID_ID)
                return@put
            }

            val request = call.receive<OrderUpdateRequest>()

            logger.info { "PUT /orders/$id to update status to ${request.status}" }

            val updatedOrder = orderService.updateOrder(id, request)
            if (updatedOrder != null) {
                call.respond(updatedOrder)
            } else {
                call.respond(HttpStatusCode.NotFound, ORDER_NOT_FOUND)
            }
        }
    }
}
