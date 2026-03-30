package com.shop.integration

import com.shop.db.AuditLogs
import com.shop.db.DatabaseFactory
import com.shop.domain.CreateOrderRequest
import com.shop.domain.OrderItemRequest
import com.shop.domain.ProductCreateRequest
import com.shop.service.AuditService
import com.shop.service.OrderService
import com.shop.service.ProductService
import com.shop.service.UserService
import com.shop.test.truncateAllTables
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresIntegrationTest {

    private lateinit var postgres: PostgreSQLContainer<*>

    @BeforeAll
    fun startPostgres() {
        assumeTrue(dockerAvailable(), "Docker недоступен — интеграционные тесты с Testcontainers пропущены")
        postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("shop")
            .withUsername("shop")
            .withPassword("shop")
        postgres.start()
        DatabaseFactory.init(postgres.jdbcUrl, postgres.username, postgres.password)
    }

    @AfterAll
    fun stopPostgres() {
        if (::postgres.isInitialized && postgres.isRunning) {
            postgres.stop()
        }
    }

    @BeforeEach
    fun clean() {
        if (!::postgres.isInitialized || !postgres.isRunning) return
        truncateAllTables()
    }

    @Test
    fun `creating order decreases stock and writes audit`() {
        val audit = AuditService()
        val users = UserService(audit)
        val products = ProductService()
        val orders = OrderService(audit)

        val userId = users.register("buyer@integration.test", "password1")
        val product = products.create(ProductCreateRequest("Notebook", null, "15.50", 10))
        val order = orders.createOrder(
            userId,
            CreateOrderRequest(listOf(OrderItemRequest(product.id, 3))),
        )

        assertEquals(1, order.items.size)
        assertEquals(7, products.getById(product.id).stock)

        transaction {
            val rows = AuditLogs.selectAll().where { AuditLogs.action eq "ORDER_CREATED" }.toList()
            assertEquals(1, rows.size)
            assertEquals(userId, rows[0][AuditLogs.userId]?.value)
        }
    }

    @Test
    fun `cancelling pending order restores stock`() {
        val audit = AuditService()
        val users = UserService(audit)
        val products = ProductService()
        val orders = OrderService(audit)

        val userId = users.register("buyer2@integration.test", "password1")
        val product = products.create(ProductCreateRequest("Mug", null, "5.00", 4))
        val order = orders.createOrder(
            userId,
            CreateOrderRequest(listOf(OrderItemRequest(product.id, 2))),
        )
        assertEquals(2, products.getById(product.id).stock)

        orders.cancelOrder(order.id, userId)
        assertEquals(4, products.getById(product.id).stock)
    }

    private fun dockerAvailable(): Boolean =
        try {
            DockerClientFactory.instance().isDockerAvailable()
        } catch (_: Throwable) {
            false
        }
}
