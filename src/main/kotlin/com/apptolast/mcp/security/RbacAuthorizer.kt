package com.apptolast.mcp.security

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Role-Based Access Control (RBAC) Authorizer for MCP Tools
 *
 * Defines scopes that control access to different tools.
 * Scopes are included in the JWT token's "scope" claim.
 *
 * Scope format: action:resource
 * Example: read:filesystem, write:database, execute:bash
 */
object RbacAuthorizer {

    /**
     * Scope definitions mapping tools to required scopes
     */
    private val toolScopes: Map<String, Set<String>> = mapOf(
        // Filesystem tools
        "readFile" to setOf("read:filesystem", "admin:*"),
        "listDirectory" to setOf("read:filesystem", "admin:*"),
        "writeFile" to setOf("write:filesystem", "admin:*"),
        "createDirectory" to setOf("write:filesystem", "admin:*"),
        "deleteFile" to setOf("write:filesystem", "admin:*"),

        // Bash execution
        "execute" to setOf("execute:bash", "admin:*"),

        // GitHub tools
        "status" to setOf("read:github", "admin:*"),
        "commit" to setOf("write:github", "admin:*"),
        "push" to setOf("write:github", "admin:*"),
        "clone" to setOf("write:github", "admin:*"),
        "log" to setOf("read:github", "admin:*"),
        "branch" to setOf("read:github", "write:github", "admin:*"),

        // Memory/Knowledge Graph tools
        "createEntities" to setOf("write:memory", "admin:*"),
        "createRelations" to setOf("write:memory", "admin:*"),
        "searchNodes" to setOf("read:memory", "admin:*"),
        "openNodes" to setOf("read:memory", "admin:*"),

        // Database tools - PostgreSQL
        "postgresQuery" to setOf("read:database", "admin:*"),
        "postgresGetSchema" to setOf("read:database", "admin:*"),
        "postgresTestConnection" to setOf("read:database", "admin:*"),

        // Database tools - MongoDB
        "mongoFind" to setOf("read:database", "admin:*"),
        "mongoListCollections" to setOf("read:database", "admin:*"),
        "mongoCount" to setOf("read:database", "admin:*"),
        "mongoAggregate" to setOf("read:database", "admin:*"),
        "mongoTestConnection" to setOf("read:database", "admin:*"),

        // Resource tools
        "resourcesList" to setOf("read:resources", "admin:*"),
        "resourcesRead" to setOf("read:resources", "admin:*"),
        "resourcesCreate" to setOf("write:resources", "admin:*"),
        "resourcesDelete" to setOf("write:resources", "admin:*")
    )

    /**
     * All available scopes
     */
    val allScopes: Set<String> = setOf(
        "read:filesystem",
        "write:filesystem",
        "execute:bash",
        "read:github",
        "write:github",
        "read:memory",
        "write:memory",
        "read:database",
        "read:resources",
        "write:resources",
        "admin:*"  // Admin scope grants access to everything
    )

    /**
     * Check if the given scopes are authorized to use the specified tool
     *
     * @param toolName The name of the tool to check
     * @param userScopes The scopes from the JWT token
     * @return true if authorized, false otherwise
     */
    fun isAuthorized(toolName: String, userScopes: Set<String>): Boolean {
        // If no scopes required for tool (unknown tool), deny by default
        val requiredScopes = toolScopes[toolName]
        if (requiredScopes == null) {
            logger.warn { "Unknown tool '$toolName' - access denied by default" }
            return false
        }

        // Check if user has admin scope (grants all access)
        if (userScopes.contains("admin:*")) {
            logger.debug { "Admin access granted for tool '$toolName'" }
            return true
        }

        // Check if user has any of the required scopes
        val hasAccess = userScopes.any { it in requiredScopes }

        if (hasAccess) {
            logger.debug { "Access granted for tool '$toolName' with scopes: $userScopes" }
        } else {
            logger.warn { "Access DENIED for tool '$toolName'. User scopes: $userScopes, Required: $requiredScopes" }
        }

        return hasAccess
    }

    /**
     * Get the required scopes for a tool
     */
    fun getRequiredScopes(toolName: String): Set<String> {
        return toolScopes[toolName] ?: emptySet()
    }

    /**
     * Parse scope string from JWT claim
     * Scopes can be space-separated or comma-separated
     */
    fun parseScopes(scopeString: String?): Set<String> {
        if (scopeString.isNullOrBlank()) return emptySet()

        return scopeString
            .replace(",", " ")
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    /**
     * Validate that all scopes in the set are valid
     */
    fun validateScopes(scopes: Set<String>): Boolean {
        val invalidScopes = scopes - allScopes
        if (invalidScopes.isNotEmpty()) {
            logger.warn { "Invalid scopes detected: $invalidScopes" }
            return false
        }
        return true
    }
}
