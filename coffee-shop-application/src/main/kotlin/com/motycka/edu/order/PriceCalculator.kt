package com.motycka.edu.order

import com.motycka.edu.menu.MenuItemDTO

object PriceCalculator {

    fun calculatePrice(menuItems: List<MenuItemDTO>, discountInPercent: Double, orderItems: List<OrderItemDTO>): Double {
        val rawTotal = orderItems.sumOf { item ->
            val menuItem = menuItems.find { it.id == item.menuItemId }
                ?: error("Menu item not found for id: ${item.menuItemId}")
            menuItem.price * item.quantity
        }
        return rawTotal * (1 - discountInPercent / 100)
    }

}
