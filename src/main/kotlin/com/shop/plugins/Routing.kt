package com.shop.plugins

import com.shop.domain.CreateOrderRequest
import com.shop.domain.LoginRequest
import com.shop.domain.ProductCreateRequest
import com.shop.domain.ProductUpdateRequest
import com.shop.domain.RegisterRequest
import com.shop.domain.UserRole
import com.shop.errors.AppException
import com.shop.errors.ForbiddenException
import com.shop.security.JwtService
import com.shop.security.UserPrincipal
import com.shop.service.OrderService
import com.shop.service.ProductService
import com.shop.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.configureRoutes(
    userService: UserService,
    jwtService: JwtService,
    productService: ProductService,
    orderService: OrderService,
) {
    route("/auth") {
        post("/register") {
            val body = call.receive<RegisterRequest>()
            val id = userService.register(body.email, body.password)
            val token = jwtService.createToken(id, UserRole.USER)
            call.respond(
                HttpStatusCode.Created,
                com.shop.domain.AuthResponse(token, id, UserRole.USER.name),
            )
        }
        post("/login") {
            val body = call.receive<LoginRequest>()
            val (id, role) = userService.login(body.email, body.password)
            val token = jwtService.createToken(id, role)
            call.respond(com.shop.domain.AuthResponse(token, id, role.name))
        }
    }

    route("/products") {
        get {
            call.respond(productService.list())
        }
        get("{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw AppException(HttpStatusCode.BadRequest, "Invalid id")
            call.respond(productService.getById(id))
        }
        authenticate("auth-jwt") {
            post {
                val p = call.requireUserPrincipal()
                p.requireAdmin()
                val body = call.receive<ProductCreateRequest>()
                call.respond(HttpStatusCode.Created, productService.create(body))
            }
            put("{id}") {
                val p = call.requireUserPrincipal()
                p.requireAdmin()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid id")
                val body = call.receive<ProductUpdateRequest>()
                call.respond(productService.update(id, body))
            }
            delete("{id}") {
                val p = call.requireUserPrincipal()
                p.requireAdmin()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid id")
                productService.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    authenticate("auth-jwt") {
        route("/orders") {
            get {
                val p = call.requireUserPrincipal()
                call.respond(orderService.listForUser(p.userId))
            }
            post {
                val p = call.requireUserPrincipal()
                val body = call.receive<CreateOrderRequest>()
                call.respond(HttpStatusCode.Created, orderService.createOrder(p.userId, body))
            }
            delete("{id}") {
                val p = call.requireUserPrincipal()
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw AppException(HttpStatusCode.BadRequest, "Invalid id")
                orderService.cancelOrder(id, p.userId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
        get("/stats/orders") {
            val p = call.requireUserPrincipal()
            p.requireAdmin()
            call.respond(orderService.orderStats())
        }
    }
}

private fun ApplicationCall.requireUserPrincipal(): UserPrincipal =
    principal<UserPrincipal>() ?: throw com.shop.errors.UnauthorizedException()

private fun UserPrincipal.requireAdmin() {
    if (role != UserRole.ADMIN) throw ForbiddenException("Admin only")
}
