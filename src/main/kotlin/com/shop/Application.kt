package com.shop

import com.shop.config.toAppConfig
import com.shop.db.DatabaseFactory
import com.shop.plugins.configureRoutes
import com.shop.plugins.configureSecurity
import com.shop.plugins.configureStatusPages
import com.shop.security.JwtService
import com.shop.serialization.appJson
import com.shop.service.AuditService
import com.shop.service.OrderService
import com.shop.service.ProductService
import com.shop.service.UserService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val appConfig = environment.config.toAppConfig()
    DatabaseFactory.init(
        jdbcUrl = appConfig.database.jdbcUrl,
        user = appConfig.database.user,
        password = appConfig.database.password,
    )

    val auditService = AuditService()
    val userService = UserService(auditService)
    userService.ensureAdmin(appConfig.admin.email, appConfig.admin.password)

    val jwtService = JwtService(appConfig.jwt)
    val productService = ProductService()
    val orderService = OrderService(auditService)

    install(CallLogging)
    install(ContentNegotiation) {
        json(appJson())
    }
    install(CORS) {
        anyHost()
    }
    configureStatusPages()
    configureSecurity(appConfig.jwt, jwtService)

    routing {
        configureRoutes(userService, jwtService, productService, orderService)
    }
}
