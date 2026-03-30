package com.shop.service

import com.shop.db.Products
import com.shop.domain.ProductCreateRequest
import com.shop.domain.ProductDto
import com.shop.domain.ProductUpdateRequest
import com.shop.errors.BadRequestException
import com.shop.errors.NotFoundException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

class ProductService {
    fun list(): List<ProductDto> = transaction {
        Products.selectAll().map { it.toDto() }
    }

    fun getById(id: Long): ProductDto = transaction {
        Products.selectAll().where { Products.id eq id }.singleOrNull()?.toDto()
            ?: throw NotFoundException("Product not found")
    }

    fun create(req: ProductCreateRequest): ProductDto {
        val price = parsePrice(req.price)
        if (req.name.isBlank()) throw BadRequestException("Name is required")
        if (req.stock < 0) throw BadRequestException("Stock cannot be negative")
        return transaction {
            val id = Products.insert {
                it[Products.name] = req.name.trim()
                it[Products.description] = req.description?.trim()
                it[Products.price] = price
                it[Products.stock] = req.stock
            } get Products.id
            getById(id.value)
        }
    }

    fun update(id: Long, req: ProductUpdateRequest): ProductDto = transaction {
        val exists = Products.selectAll().where { Products.id eq id }.singleOrNull()
            ?: throw NotFoundException("Product not found")
        val newName = req.name?.trim() ?: exists[Products.name]
        val newDesc = req.description ?: exists[Products.description]
        val newPrice = req.price?.let { parsePrice(it) } ?: exists[Products.price]
        val newStock = req.stock ?: exists[Products.stock]
        if (newStock < 0) throw BadRequestException("Stock cannot be negative")
        Products.update({ Products.id eq id }) {
            it[Products.name] = newName
            it[Products.description] = newDesc
            it[Products.price] = newPrice
            it[Products.stock] = newStock
        }
        getById(id)
    }

    fun delete(id: Long) {
        transaction {
            val deleted = Products.deleteWhere { Products.id eq id }
            if (deleted == 0) throw NotFoundException("Product not found")
        }
    }

    private fun parsePrice(raw: String): BigDecimal =
        try {
            BigDecimal(raw.trim())
        } catch (_: Exception) {
            throw BadRequestException("Invalid price")
        }

    private fun org.jetbrains.exposed.sql.ResultRow.toDto() = ProductDto(
        id = this[Products.id].value,
        name = this[Products.name],
        description = this[Products.description],
        price = this[Products.price].toPlainString(),
        stock = this[Products.stock],
    )
}
