package com.shop.db

import com.shop.domain.OrderStatus
import com.shop.domain.UserRole
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.OffsetDateTime

object Users : LongIdTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = enumerationByName("role", 16, UserRole::class)
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
}

object Products : LongIdTable("products") {
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val price = decimal("price", 12, 2)
    val stock = integer("stock")
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    init {
        index(false, name)
    }
}

object Orders : LongIdTable("orders") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val status = enumerationByName("status", 32, OrderStatus::class)
    val totalAmount = decimal("total_amount", 14, 2)
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    init {
        index(false, userId)
        index(false, status)
    }
}

object OrderItems : LongIdTable("order_items") {
    val orderId = reference("order_id", Orders, onDelete = ReferenceOption.CASCADE)
    val productId = reference("product_id", Products, onDelete = ReferenceOption.RESTRICT)
    val quantity = integer("quantity")
    val unitPrice = decimal("unit_price", 12, 2)
    init {
        index(false, orderId)
        index(false, productId)
    }
}

object AuditLogs : LongIdTable("audit_logs") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.SET_NULL).nullable()
    val action = varchar("action", 128)
    val entityType = varchar("entity_type", 64)
    val entityId = long("entity_id").nullable()
    val details = text("details").nullable()
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    init {
        index(false, entityType, entityId)
        index(false, createdAt)
    }
}
