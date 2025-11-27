package com.apptolast.mcp.security

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

private val logger = KotlinLogging.logger {}

/**
 * MCP Principal containing user information from JWT
 */
data class McpPrincipal(
    val subject: String,
    val scopes: Set<String>,
    val issuer: String,
    val audience: List<String>
) : Principal

/**
 * Configure JWT authentication for the MCP Server
 *
 * This plugin:
 * 1. Validates JWT tokens using RS256 (public key verification)
 * 2. Extracts claims and creates McpPrincipal
 * 3. Handles authentication failures gracefully
 *
 * Reference: https://ktor.io/docs/server-jwt.html
 */
fun Application.configureJwtAuth() {
    // Log JWT configuration at startup
    JwtConfig.logConfiguration()

    // Skip authentication setup if not enabled
    if (!JwtConfig.isEnabled()) {
        logger.warn { "JWT Authentication is DISABLED - all endpoints are PUBLIC" }
        return
    }

    val verifier = JwtConfig.verifier ?: run {
        logger.error { "JWT verifier not available - authentication disabled" }
        return
    }

    install(Authentication) {
        jwt("mcp-auth") {
            realm = JwtConfig.realm

            verifier(verifier)

            validate { credential ->
                try {
                    val payload = credential.payload

                    // Extract required claims
                    val subject = payload.subject
                    if (subject.isNullOrBlank()) {
                        logger.warn { "JWT validation failed: missing subject claim" }
                        return@validate null
                    }

                    // Verify audience
                    val audiences = payload.audience ?: emptyList()
                    if (!audiences.contains(JwtConfig.audience)) {
                        logger.warn { "JWT validation failed: invalid audience. Expected: ${JwtConfig.audience}, Got: $audiences" }
                        return@validate null
                    }

                    // Extract scopes from 'scope' claim
                    val scopeString = payload.getClaim("scope")?.asString()
                    val scopes = RbacAuthorizer.parseScopes(scopeString)

                    logger.debug { "JWT validated successfully for subject: $subject with scopes: $scopes" }

                    McpPrincipal(
                        subject = subject,
                        scopes = scopes,
                        issuer = payload.issuer ?: "",
                        audience = audiences
                    )
                } catch (e: Exception) {
                    logger.error(e) { "JWT validation error" }
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "error" to "Unauthorized",
                        "message" to "Invalid or missing authentication token",
                        "realm" to JwtConfig.realm
                    )
                )
            }
        }
    }

    logger.info { "JWT Authentication configured successfully" }
}

/**
 * Extension function to get McpPrincipal from authenticated routes
 */
fun ApplicationCall.mcpPrincipal(): McpPrincipal? {
    return principal<McpPrincipal>()
}

/**
 * Extension function to check if request is authenticated
 */
fun ApplicationCall.isAuthenticated(): Boolean {
    return mcpPrincipal() != null
}

/**
 * Extension function to check if authenticated user has required scope
 */
fun ApplicationCall.hasScope(scope: String): Boolean {
    val principal = mcpPrincipal() ?: return false
    return principal.scopes.contains(scope) || principal.scopes.contains("admin:*")
}

/**
 * Extension function to check if authenticated user can use a tool
 */
fun ApplicationCall.canUseTool(toolName: String): Boolean {
    val principal = mcpPrincipal() ?: return false
    return RbacAuthorizer.isAuthorized(toolName, principal.scopes)
}
