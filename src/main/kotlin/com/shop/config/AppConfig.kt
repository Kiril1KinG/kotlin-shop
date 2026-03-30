package com.shop.config

import io.ktor.server.config.ApplicationConfig

data class JwtSettings(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
)

data class AdminSeed(
    val email: String,
    val password: String,
)

data class DatabaseSettings(
    val jdbcUrl: String,
    val user: String,
    val password: String,
)

data class AppConfig(
    val jwt: JwtSettings,
    val admin: AdminSeed,
    val database: DatabaseSettings,
)

fun ApplicationConfig.toAppConfig(): AppConfig {
    val app = config("app")
    val jwt = app.config("jwt")
    val admin = app.config("admin")
    val db = config("database")
    return AppConfig(
        jwt = JwtSettings(
            secret = envOr("JWT_SECRET", jwt.property("secret").getString()),
            issuer = jwt.property("issuer").getString(),
            audience = jwt.property("audience").getString(),
            realm = jwt.property("realm").getString(),
        ),
        admin = AdminSeed(
            email = envOr("ADMIN_EMAIL", admin.property("email").getString()),
            password = envOr("ADMIN_PASSWORD", admin.property("password").getString()),
        ),
        database = DatabaseSettings(
            jdbcUrl = envOr("JDBC_URL", db.property("jdbcUrl").getString()),
            user = envOrFirst("DATABASE_USER", "POSTGRES_USER", fallback = db.property("user").getString()),
            password = envOrFirst("DATABASE_PASSWORD", "POSTGRES_PASSWORD", fallback = db.property("password").getString()),
        ),
    )
}

private fun envOr(name: String, fallback: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback

private fun envOrFirst(vararg names: String, fallback: String): String {
    for (name in names) {
        val v = System.getenv(name)
        if (v != null) return v
    }
    return fallback
}
