package com.shop.domain

import kotlinx.serialization.Serializable

enum class UserRole {
    USER,
    ADMIN,
}

enum class OrderStatus {
    PENDING,
    CANCELLED,
    COMPLETED,
}

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Long,
    val role: String,
)

@Serializable
data class ProductDto(
    val id: Long,
    val name: String,
    val description: String?,
    val price: String,
    val stock: Int,
)

@Serializable
data class ProductCreateRequest(
    val name: String,
    val description: String? = null,
    val price: String,
    val stock: Int,
)

@Serializable
data class ProductUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val price: String? = null,
    val stock: Int? = null,
)

@Serializable
data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)

@Serializable
data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
)

@Serializable
data class OrderItemDto(
    val productId: Long,
    val quantity: Int,
    val unitPrice: String,
)

@Serializable
data class OrderDto(
    val id: Long,
    val status: String,
    val totalAmount: String,
    val items: List<OrderItemDto>,
    val createdAt: String,
)

@Serializable
data class OrderStatsDto(
    val totalOrders: Long,
    val totalRevenue: String,
    val byStatus: Map<String, Long>,
)
