package com.shop.service

import com.shop.db.OrderItems
import com.shop.db.Orders
import com.shop.db.Products
import com.shop.db.Users
import com.shop.domain.CreateOrderRequest
import com.shop.domain.OrderDto
import com.shop.domain.OrderItemDto
import com.shop.domain.OrderStatsDto
import com.shop.domain.OrderStatus
import com.shop.errors.BadRequestException
import com.shop.errors.ForbiddenException
import com.shop.errors.NotFoundException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class OrderService(
    private val auditService: AuditService,
) {
    private val isoFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun createOrder(userId: Long, req: CreateOrderRequest): OrderDto {
        if (req.items.isEmpty()) throw BadRequestException("Order must contain at least one item")
        return transaction {
            val lines = mutableListOf<Triple<Long, Int, BigDecimal>>()
            var total = BigDecimal.ZERO
            for (line in req.items) {
                if (line.quantity <= 0) throw BadRequestException("Quantity must be positive")
                val row = Products.selectAll().where { Products.id eq line.productId }.forUpdate()
                    .singleOrNull() ?: throw NotFoundException("Product ${line.productId} not found")
                val stock = row[Products.stock]
                if (stock < line.quantity) {
                    throw BadRequestException("Insufficient stock for product ${line.productId}")
                }
                val unitPrice = row[Products.price]
                total = total.add(unitPrice.multiply(BigDecimal(line.quantity)))
                lines.add(Triple(line.productId, line.quantity, unitPrice))
            }
            for ((productId, qty, _) in lines) {
                val current = Products.selectAll().where { Products.id eq productId }.single()[Products.stock]
                Products.update({ Products.id eq productId }) {
                    it[Products.stock] = current - qty
                }
            }
            val orderId = Orders.insert {
                it[Orders.userId] = EntityID(userId, Users)
                it[Orders.status] = OrderStatus.PENDING
                it[Orders.totalAmount] = total
            } get Orders.id
            for ((productId, qty, unitPrice) in lines) {
                OrderItems.insert {
                    it[OrderItems.orderId] = EntityID(orderId.value, Orders)
                    it[OrderItems.productId] = EntityID(productId, Products)
                    it[OrderItems.quantity] = qty
                    it[OrderItems.unitPrice] = unitPrice
                }
            }
            auditService.log(
                userId = userId,
                action = "ORDER_CREATED",
                entityType = "ORDER",
                entityId = orderId.value,
                details = "items=${lines.size},total=$total",
            )
            loadOrderDto(orderId.value)
        }
    }

    fun listForUser(userId: Long): List<OrderDto> = transaction {
        Orders.selectAll().where { Orders.userId eq EntityID(userId, Users) }
            .map { it[Orders.id].value }
            .map { loadOrderDto(it) }
    }

    fun cancelOrder(orderId: Long, userId: Long) {
        transaction {
            val orderRow = Orders.selectAll().where { Orders.id eq orderId }.forUpdate().singleOrNull()
                ?: throw NotFoundException("Order not found")
            if (orderRow[Orders.userId].value != userId) {
                throw ForbiddenException("Not your order")
            }
            if (orderRow[Orders.status] != OrderStatus.PENDING) {
                throw BadRequestException("Only pending orders can be cancelled")
            }
            val items = OrderItems.selectAll().where { OrderItems.orderId eq EntityID(orderId, Orders) }
            for (item in items) {
                val pid = item[OrderItems.productId].value
                val qty = item[OrderItems.quantity]
                val current = Products.selectAll().where { Products.id eq pid }.single()[Products.stock]
                Products.update({ Products.id eq pid }) {
                    it[Products.stock] = current + qty
                }
            }
            Orders.update({ Orders.id eq orderId }) {
                it[Orders.status] = OrderStatus.CANCELLED
            }
            auditService.log(
                userId = userId,
                action = "ORDER_CANCELLED",
                entityType = "ORDER",
                entityId = orderId,
                details = null,
            )
        }
    }

    fun orderStats(): OrderStatsDto = transaction {
        val rows = Orders.selectAll().toList()
        val totalOrders = rows.size.toLong()
        val totalRevenue = rows
            .filter { it[Orders.status] != OrderStatus.CANCELLED }
            .fold(BigDecimal.ZERO) { acc, r -> acc.add(r[Orders.totalAmount]) }
        val byStatus = rows
            .groupingBy { it[Orders.status].name }
            .eachCount()
            .mapValues { it.value.toLong() }
        OrderStatsDto(
            totalOrders = totalOrders,
            totalRevenue = totalRevenue.toPlainString(),
            byStatus = byStatus,
        )
    }

    private fun loadOrderDto(orderId: Long): OrderDto {
        val o = Orders.selectAll().where { Orders.id eq orderId }.singleOrNull()
            ?: throw NotFoundException("Order not found")
        val items = OrderItems.selectAll().where { OrderItems.orderId eq EntityID(orderId, Orders) }.map {
            OrderItemDto(
                productId = it[OrderItems.productId].value,
                quantity = it[OrderItems.quantity],
                unitPrice = it[OrderItems.unitPrice].toPlainString(),
            )
        }
        val created = o[Orders.createdAt] as OffsetDateTime
        val createdStr = created.format(isoFmt)
        return OrderDto(
            id = o[Orders.id].value,
            status = o[Orders.status].name,
            totalAmount = o[Orders.totalAmount].toPlainString(),
            items = items,
            createdAt = createdStr,
        )
    }
}
