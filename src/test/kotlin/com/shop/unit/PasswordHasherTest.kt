package com.shop.unit

import com.shop.security.PasswordHasher
import kotlin.test.Test
import kotlin.test.assertTrue

class PasswordHasherTest {

    @Test
    fun `verify succeeds for same password`() {
        val hash = PasswordHasher.hash("my-secret-password")
        assertTrue(PasswordHasher.verify("my-secret-password", hash))
    }
}
