package com.shop.service

import com.shop.db.Users
import com.shop.domain.UserRole
import com.shop.errors.BadRequestException
import com.shop.errors.ConflictException
import com.shop.errors.UnauthorizedException
import com.shop.security.PasswordHasher
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserService(
    private val auditService: AuditService,
) {
    fun register(email: String, password: String): Long {
        if (email.isBlank() || !email.contains('@')) {
            throw BadRequestException("Invalid email")
        }
        if (password.length < 6) {
            throw BadRequestException("Password must be at least 6 characters")
        }
        return transaction {
            val exists = Users.selectAll().where { Users.email eq email }.count() > 0
            if (exists) {
                throw ConflictException("Email already registered")
            }
            val hash = PasswordHasher.hash(password)
            val id = Users.insert {
                it[Users.email] = email.trim().lowercase()
                it[Users.passwordHash] = hash
                it[Users.role] = UserRole.USER
            } get Users.id
            auditService.log(
                userId = id.value,
                action = "USER_REGISTERED",
                entityType = "USER",
                entityId = id.value,
                details = null,
            )
            id.value
        }
    }

    fun login(email: String, password: String): Pair<Long, UserRole> {
        return transaction {
            val row = Users.selectAll().where { Users.email eq email.trim().lowercase() }.singleOrNull()
                ?: throw UnauthorizedException("Invalid credentials")
            if (!PasswordHasher.verify(password, row[Users.passwordHash])) {
                throw UnauthorizedException("Invalid credentials")
            }
            row[Users.id].value to row[Users.role]
        }
    }

    fun ensureAdmin(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return
        transaction {
            val existing = Users.selectAll().where { Users.email eq email }.singleOrNull()
            if (existing != null) return@transaction
            Users.insert {
                it[Users.email] = email
                it[Users.passwordHash] = PasswordHasher.hash(password)
                it[Users.role] = UserRole.ADMIN
            }
        }
    }
}
