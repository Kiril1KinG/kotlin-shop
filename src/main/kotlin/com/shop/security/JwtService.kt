package com.shop.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.shop.config.JwtSettings
import com.shop.domain.UserRole
import java.util.Date

class JwtService(
    private val settings: JwtSettings,
) {
    private val algorithm: Algorithm by lazy { Algorithm.HMAC256(settings.secret) }

    fun verifier(): JWTVerifier =
        JWT.require(algorithm)
            .withIssuer(settings.issuer)
            .withAudience(settings.audience)
            .build()

    fun createToken(userId: Long, role: UserRole): String {
        val now = Date()
        val expiry = Date(now.time + TOKEN_TTL_MS)
        return JWT.create()
            .withIssuer(settings.issuer)
            .withAudience(settings.audience)
            .withSubject(userId.toString())
            .withClaim("role", role.name)
            .withIssuedAt(now)
            .withExpiresAt(expiry)
            .sign(algorithm)
    }

    companion object {
        private const val TOKEN_TTL_MS = 86_400_000L // 24h
    }
}
