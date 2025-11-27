package com.apptolast.mcp.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

private val logger = KotlinLogging.logger {}

/**
 * JWT Configuration for MCP Server Authentication
 *
 * Uses RS256 (RSA with SHA-256) for asymmetric signing:
 * - Frontend signs tokens with private key
 * - MCP Server verifies tokens with public key
 *
 * Reference: https://auth0.com/docs/secure/tokens/json-web-tokens/json-web-token-claims
 */
object JwtConfig {
    // JWT Configuration from environment variables
    val issuer: String = System.getenv("JWT_ISSUER") ?: "mcp-frontend"
    val audience: String = System.getenv("JWT_AUDIENCE") ?: "mcp-server"
    val realm: String = System.getenv("JWT_REALM") ?: "MCP Server"

    // Token expiration in minutes (default: 15 minutes)
    val expirationMinutes: Long = System.getenv("JWT_EXPIRATION_MINUTES")?.toLongOrNull() ?: 15

    // RSA Keys loaded from environment
    private val publicKeyPem: String? = System.getenv("JWT_PUBLIC_KEY")
    private val privateKeyPem: String? = System.getenv("JWT_PRIVATE_KEY")

    // Parsed RSA keys
    val publicKey: RSAPublicKey? by lazy { parsePublicKey(publicKeyPem) }
    val privateKey: RSAPrivateKey? by lazy { parsePrivateKey(privateKeyPem) }

    // RS256 Algorithm (only needs public key for verification)
    val algorithm: Algorithm? by lazy {
        publicKey?.let { Algorithm.RSA256(it, null) }
    }

    // JWT Verifier
    val verifier: JWTVerifier? by lazy {
        algorithm?.let {
            JWT.require(it)
                .withIssuer(issuer)
                .withAudience(audience)
                .build()
        }
    }

    /**
     * Check if JWT authentication is enabled
     * Returns false if public key is not configured (allows running without auth for development)
     */
    fun isEnabled(): Boolean {
        val enabled = publicKey != null && algorithm != null
        if (!enabled) {
            logger.warn { "JWT authentication is DISABLED - no public key configured" }
        }
        return enabled
    }

    /**
     * Parse PEM-encoded RSA public key
     */
    private fun parsePublicKey(pem: String?): RSAPublicKey? {
        if (pem.isNullOrBlank()) {
            logger.info { "No JWT public key configured" }
            return null
        }

        return try {
            val keyContent = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")

            val keyBytes = Base64.getDecoder().decode(keyContent)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")

            logger.info { "Successfully loaded JWT public key" }
            keyFactory.generatePublic(keySpec) as RSAPublicKey
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse JWT public key" }
            null
        }
    }

    /**
     * Parse PEM-encoded RSA private key
     * Note: Private key is only needed if this server also generates tokens
     */
    private fun parsePrivateKey(pem: String?): RSAPrivateKey? {
        if (pem.isNullOrBlank()) {
            return null
        }

        return try {
            val keyContent = pem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")

            val keyBytes = Base64.getDecoder().decode(keyContent)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")

            logger.info { "Successfully loaded JWT private key" }
            keyFactory.generatePrivate(keySpec) as RSAPrivateKey
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse JWT private key" }
            null
        }
    }

    /**
     * Log current JWT configuration (without exposing keys)
     */
    fun logConfiguration() {
        logger.info { "JWT Configuration:" }
        logger.info { "  - Issuer: $issuer" }
        logger.info { "  - Audience: $audience" }
        logger.info { "  - Realm: $realm" }
        logger.info { "  - Expiration: $expirationMinutes minutes" }
        logger.info { "  - Public Key: ${if (publicKey != null) "LOADED" else "NOT CONFIGURED"}" }
        logger.info { "  - Private Key: ${if (privateKey != null) "LOADED" else "NOT CONFIGURED"}" }
        logger.info { "  - Authentication: ${if (isEnabled()) "ENABLED" else "DISABLED"}" }
    }
}
