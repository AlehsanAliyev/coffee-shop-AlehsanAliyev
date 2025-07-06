package com.motycka.edu.order

import com.motycka.edu.customer.InternalCustomerService
import com.motycka.edu.menu.InternalMenuService


/*

I added this file to isolate business logic from routing.
It coordinates access to the OrderRepository, OrderItemRepository, and MenuService,
 and applies the discount logic based on customer data. This layer improves maintainability, testability,
and keeps our HTTP routes clean and focused on request handling.
 */


class OrderService(
    private val orderRepo: OrderRepository,
    private val orderItemRepo: OrderItemRepository,
    private val menuService: InternalMenuService,
    private val customerService: InternalCustomerService
) {
    fun getAllOrders(): List<OrderResponse> {
        return orderRepo.selectAll().map { order ->
            val items = orderItemRepo.selectByOrderId(order.id!!)
            val menuItems = menuService.getMenuItems(items.map { it.menuItemId }.toSet())
            toResponse(order, items, menuItems)
        }
    }

    fun getOrderById(id: OrderId): OrderResponse? {
        val order = orderRepo.selectById(id) ?: return null
        val items = orderItemRepo.selectByOrderId(order.id!!)
        val menuItems = menuService.getMenuItems(items.map { it.menuItemId }.toSet())
        return toResponse(order, items, menuItems)
    }

    fun createOrder(customerId: Long, request: OrderRequest): OrderResponse {
        val items = request.items.map {
            OrderItemDTO(null, 0, it.menuItemId, it.quantity)
        }

        val orderDTO = orderRepo.create(OrderDTO(null, customerId, OrderStatus.PENDING))
        val itemsWithOrderId = items.map { it.copy(orderId = orderDTO.id!!) }

        orderItemRepo.createOrderItems(itemsWithOrderId)

        val menuItems = menuService.getMenuItems(itemsWithOrderId.map { it.menuItemId }.toSet())
        val discount = customerService.getDiscountPercent(customerId)
        val price = PriceCalculator.calculatePrice(menuItems.toList(), discount, itemsWithOrderId)

        return toResponse(orderDTO, itemsWithOrderId, menuItems, price)
    }

    fun updateOrder(id: OrderId, request: OrderUpdateRequest): OrderResponse? {
        val existing = orderRepo.selectById(id) ?: return null
        val updated = existing.copy(status = request.status)
        val newOrder = orderRepo.update(updated)
        val items = orderItemRepo.selectByOrderId(id)
        val menuItems = menuService.getMenuItems(items.map { it.menuItemId }.toSet())
        return toResponse(newOrder, items, menuItems)
    }

    private fun toResponse(
        order: OrderDTO,
        items: List<OrderItemDTO>,
        menuItems: Collection<MenuItemDTO>,
        total: Double = PriceCalculator.calculatePrice(menuItems.toList(), 0.0, items)
    ): OrderResponse {
        val itemResponses = items.map { item ->
            val menuItem = menuItems.first { it.id == item.menuItemId }
            OrderItemResponse(menuItem = menuItem.toResponse(), quantity = item.quantity)
        }
        return OrderResponse(
            id = order.id!!,
            menuItems = itemResponses,
            totalPrice = total,
            status = order.status
        )
    }
}
