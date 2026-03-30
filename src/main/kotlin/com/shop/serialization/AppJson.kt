package com.shop.serialization

import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun appJson(): Json = Json {
    prettyPrint = false
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}
