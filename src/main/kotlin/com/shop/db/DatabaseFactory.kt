package com.shop.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    private var pool: HikariDataSource? = null

    fun init(jdbcUrl: String, user: String, password: String) {
        pool?.close()
        val driverClassName = when {
            jdbcUrl.startsWith("jdbc:h2:", ignoreCase = true) -> "org.h2.Driver"
            else -> "org.postgresql.Driver"
        }
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            username = user
            this.password = password
            this.driverClassName = driverClassName
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        val ds = HikariDataSource(config)
        pool = ds
        Database.connect(ds)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Products,
                Orders,
                OrderItems,
                AuditLogs,
            )
        }
    }
}
