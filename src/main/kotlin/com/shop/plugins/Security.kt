package com.shop.plugins

import com.shop.config.JwtSettings
import com.shop.domain.UserRole
import com.shop.security.JwtService
import com.shop.security.UserPrincipal
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity(jwtSettings: JwtSettings, jwtService: JwtService) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtSettings.realm
            verifier(jwtService.verifier())
            validate { credential ->
                val sub = credential.payload.subject?.toLongOrNull() ?: return@validate null
                val roleName = credential.payload.getClaim("role").asString() ?: return@validate null
                val role = try {
                    UserRole.valueOf(roleName)
                } catch (_: IllegalArgumentException) {
                    return@validate null
                }
                UserPrincipal(sub, role)
            }
        }
    }
}
