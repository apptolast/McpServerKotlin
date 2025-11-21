package com.apptolast.mcp.server

import com.apptolast.mcp.util.ToolDefinition
import com.apptolast.mcp.util.ToolResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Registry for all MCP tools available in the server.
 *
 * This class manages the registration and invocation of tools that can be called
 * via the MCP protocol. Each tool is defined with:
 * - A unique name
 * - A description of what it does
 * - An input schema (JSON Schema format)
 * - A handler function that executes the tool
 */
class ToolRegistry {

    companion object {
        /**
         * Centralized static tool definitions - single source of truth for all available tools.
         *
         * This prevents drift between:
         * - McpServerInstance.kt documentation
         * - Application.kt /tools endpoint
         * - ToolListSynchronizationTest.kt
         *
         * Structure:
         * - Key: Module name (lowercase)
         * - Value: List of tool names exposed by that module
         */
        val TOOLS_BY_MODULE: Map<String, List<String>> = mapOf(
            "filesystem" to listOf(
                "readFile",
                "writeFile",
                "listDirectory",
                "createDirectory",
                "deleteFile"
            ),
            "bash" to listOf(
                "execute"
            ),
            "github" to listOf(
                "status",
                "commit",
                "push",
                "clone",
                "log",
                "branch"
            ),
            "memory" to listOf(
                "createEntities",
                "createRelations",
                "searchNodes",
                "openNodes"
            ),
            "postgresql" to listOf(
                "executeQuery",
                "getSchema",
                "testConnection"
            ),
            "mongodb" to listOf(
                "find",
                "listCollections",
                "countDocuments",
                "aggregate",
                "testConnection"
            ),
            "resources" to listOf(
                "listResources",
                "readResource",
                "createResource",
                "deleteResource"
            )
        )

        /**
         * Total number of tools across all modules.
         */
        val TOTAL_TOOLS: Int = TOOLS_BY_MODULE.values.sumOf { it.size }

        /**
         * Get tool count for a specific module.
         */
        fun getModuleToolCount(module: String): Int {
            return TOOLS_BY_MODULE[module]?.size ?: 0
        }

        /**
         * Get all module names.
         */
        fun getAllModules(): Set<String> {
            return TOOLS_BY_MODULE.keys
        }

        /**
         * Check if a tool exists in a specific module.
         */
        fun hasToolInModule(module: String, toolName: String): Boolean {
            return TOOLS_BY_MODULE[module]?.contains(toolName) ?: false
        }
    }

    private val tools = ConcurrentHashMap<String, ToolHandler>()

    /**
     * Registers a new tool in the registry
     *
     * @param name Unique identifier for the tool
     * @param description Human-readable description of what the tool does
     * @param inputSchema JSON Schema defining the expected input parameters
     * @param handler Suspending function that executes the tool and returns a ToolResult
     */
    fun register(
        name: String,
        description: String,
        inputSchema: JsonObject,
        handler: suspend (JsonObject) -> ToolResult
    ) {
        if (tools.containsKey(name)) {
            logger.warn { "Tool '$name' is already registered. Overwriting..." }
        }

        tools[name] = ToolHandler(
            definition = ToolDefinition(
                name = name,
                description = description,
                inputSchema = inputSchema
            ),
            handler = handler
        )

        logger.info { "Registered tool: $name" }
    }

    /**
     * Invokes a registered tool by name with the provided parameters
     *
     * @param name The name of the tool to invoke
     * @param params JSON object containing the input parameters for the tool
     * @return ToolResult with the execution result or error
     */
    suspend fun invoke(name: String, params: JsonObject): ToolResult {
        val tool = tools[name]

        if (tool == null) {
            logger.warn { "Tool not found: $name" }
            return ToolResult.error("Tool not found: $name")
        }

        return try {
            logger.info { "Invoking tool: $name" }
            tool.handler(params)
        } catch (e: Exception) {
            logger.error(e) { "Tool execution failed: $name" }
            ToolResult.error("Tool execution failed: ${e.message}")
        }
    }

    /**
     * Returns a list of all registered tools with their definitions
     *
     * @return List of ToolDefinition objects for all registered tools
     */
    fun listTools(): List<ToolDefinition> {
        return tools.values.map { it.definition }
    }

    /**
     * Returns the number of registered tools
     */
    fun size(): Int = tools.size

    /**
     * Checks if a tool with the given name is registered
     */
    fun hasType(name: String): Boolean = tools.containsKey(name)

    /**
     * Clears all registered tools (useful for testing)
     */
    internal fun clear() {
        tools.clear()
        logger.info { "Cleared all registered tools" }
    }

    private data class ToolHandler(
        val definition: ToolDefinition,
        val handler: suspend (JsonObject) -> ToolResult
    )
}

/**
 * Helper functions to create JSON Schema objects for tool input validation
 */
object SchemaBuilder {

    /**
     * Creates a basic JSON Schema object with the given properties
     *
     * @param type The JSON Schema type (usually "object")
     * @param properties Map of property names to their schemas
     * @param required List of required property names
     * @return JsonObject representing the schema
     */
    fun createSchema(
        type: String = "object",
        properties: Map<String, JsonObject>,
        required: List<String> = emptyList()
    ): JsonObject {
        return buildJsonObject {
            put("type", type)
            put("properties", JsonObject(properties))
            if (required.isNotEmpty()) {
                put("required", JsonArray(required.map { JsonPrimitive(it) }))
            }
        }
    }

    /**
     * Creates a simple string property schema
     */
    fun stringProperty(description: String, enum: List<String>? = null): JsonObject {
        return buildJsonObject {
            put("type", "string")
            put("description", description)
            if (enum != null) {
                put("enum", JsonArray(enum.map { JsonPrimitive(it) }))
            }
        }
    }

    /**
     * Creates a simple boolean property schema
     */
    fun booleanProperty(description: String, default: Boolean? = null): JsonObject {
        return buildJsonObject {
            put("type", "boolean")
            put("description", description)
            if (default != null) {
                put("default", default)
            }
        }
    }

    /**
     * Creates a simple integer property schema
     */
    fun integerProperty(description: String, minimum: Int? = null, maximum: Int? = null): JsonObject {
        return buildJsonObject {
            put("type", "integer")
            put("description", description)
            if (minimum != null) {
                put("minimum", minimum)
            }
            if (maximum != null) {
                put("maximum", maximum)
            }
        }
    }

    /**
     * Creates an array property schema
     */
    fun arrayProperty(description: String, items: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", "array")
            put("description", description)
            put("items", items)
        }
    }

    /**
     * Creates an object property schema
     */
    fun objectProperty(description: String, properties: Map<String, JsonObject>): JsonObject {
        return buildJsonObject {
            put("type", "object")
            put("description", description)
            put("properties", JsonObject(properties))
        }
    }

    /**
     * Creates a map/dictionary property schema (object with any string keys)
     */
    fun mapProperty(description: String, valueSchema: JsonObject): JsonObject {
        return buildJsonObject {
            put("type", "object")
            put("description", description)
            put("properties", JsonObject(emptyMap()))
            put("additionalProperties", valueSchema)
        }
    }
}
