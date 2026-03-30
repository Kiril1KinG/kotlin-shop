package com.shop.test

import com.shop.serialization.appJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import com.shop.db.AuditLogs
import com.shop.db.OrderItems
import com.shop.db.Orders
import com.shop.db.Products
import com.shop.db.Users

fun testApplicationConfig(): ApplicationConfig {
    val c = MapApplicationConfig()
    c.put("ktor.deployment.port", "0")
    c.put("app.jwt.secret", "test-secret-32-characters-long-exactly!!")
    c.put("app.jwt.issuer", "shop-api")
    c.put("app.jwt.audience", "shop-users")
    c.put("app.jwt.realm", "Shop API")
    c.put("app.admin.email", "admin-e2e@test.local")
    c.put("app.admin.password", "admin-e2e")
    c.put(
        "database.jdbcUrl",
        "jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    )
    c.put("database.user", "sa")
    c.put("database.password", "")
    return c
}

fun jsonTestClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(appJson())
    }
}

fun truncateAllTables() {
    transaction {
        OrderItems.deleteAll()
        Orders.deleteAll()
        AuditLogs.deleteAll()
        Products.deleteAll()
        Users.deleteAll()
    }
}
