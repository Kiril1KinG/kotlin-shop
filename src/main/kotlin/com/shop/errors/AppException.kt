package com.shop.errors

import io.ktor.http.HttpStatusCode

open class AppException(
    val status: HttpStatusCode,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class NotFoundException(message: String) : AppException(HttpStatusCode.NotFound, message)
class BadRequestException(message: String) : AppException(HttpStatusCode.BadRequest, message)
class UnauthorizedException(message: String = "Unauthorized") : AppException(HttpStatusCode.Unauthorized, message)
class ForbiddenException(message: String = "Forbidden") : AppException(HttpStatusCode.Forbidden, message)
class ConflictException(message: String) : AppException(HttpStatusCode.Conflict, message)
