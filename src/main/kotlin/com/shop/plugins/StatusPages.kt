package com.shop.plugins

import com.shop.errors.AppException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ErrorResponse(
    val error: String,
)

private val log = LoggerFactory.getLogger("ErrorHandler")

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<AppException> { call, cause ->
            log.warn("{}: {}", cause.status, cause.message)
            call.respond(cause.status, ErrorResponse(cause.message ?: "Error"))
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Internal server error"),
            )
        }
    }
}
