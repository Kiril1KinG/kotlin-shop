package com.shop.e2e

import com.shop.domain.RegisterRequest
import com.shop.module
import com.shop.serialization.appJson
import com.shop.test.testApplicationConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiE2ETest {

    @Test
    fun `GET products returns 200 and json array`() = runBlocking {
        testApplication {
            environment {
                config = testApplicationConfig()
            }
            application {
                module()
            }
            val client = createClient {
                install(ContentNegotiation) {
                    json(appJson())
                }
            }
            val response = client.get("/products")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().trimStart().startsWith("["))
        }
    }

    @Test
    fun `POST auth register returns 201`() = runBlocking {
        testApplication {
            environment {
                config = testApplicationConfig()
            }
            application {
                module()
            }
            val client = createClient {
                install(ContentNegotiation) {
                    json(appJson())
                }
            }
            val response = client.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest("e2e-${System.nanoTime()}@test.local", "secret12"))
            }
            assertEquals(HttpStatusCode.Created, response.status)
            assertTrue(response.bodyAsText().contains("token"))
        }
    }
}
