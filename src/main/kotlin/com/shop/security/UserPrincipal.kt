package com.shop.security

import com.shop.domain.UserRole
import io.ktor.server.auth.*

data class UserPrincipal(
    val userId: Long,
    val role: UserRole,
) : Principal
