package com.shop.security

import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
    fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(12))

    fun verify(password: String, hash: String): Boolean =
        try {
            BCrypt.checkpw(password, hash)
        } catch (_: IllegalArgumentException) {
            false
        }
}
