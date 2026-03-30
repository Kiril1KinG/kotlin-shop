package com.shop.unit

import com.auth0.jwt.JWT
import com.shop.config.JwtSettings
import com.shop.domain.UserRole
import com.shop.security.JwtService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JwtServiceTest {

    private val settings = JwtSettings(
        secret = "unit-test-secret-32-characters-long!!",
        issuer = "shop-api",
        audience = "shop-users",
        realm = "Shop API",
    )

    private val jwtService = JwtService(settings)

    @Test
    fun `token contains subject and role`() {
        val token = jwtService.createToken(99L, UserRole.USER)
        val decoded = JWT.decode(token)
        assertEquals("99", decoded.subject)
        assertEquals(UserRole.USER.name, decoded.getClaim("role").asString())
        assertEquals(settings.issuer, decoded.issuer)
    }

    @Test
    fun `verifier accepts own token`() {
        val token = jwtService.createToken(1L, UserRole.ADMIN)
        val jwt = jwtService.verifier().verify(token)
        assertNotNull(jwt)
        assertEquals("1", jwt.subject)
    }
}
