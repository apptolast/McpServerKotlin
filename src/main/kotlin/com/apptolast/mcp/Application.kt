package com.apptolast.mcp

import com.apptolast.mcp.modules.bash.BashExecutor
import com.apptolast.mcp.modules.database.MongoDBModule
import com.apptolast.mcp.modules.database.PostgreSQLModule
import com.apptolast.mcp.modules.filesystem.FilesystemModule
import com.apptolast.mcp.modules.github.GitHubModule
import com.apptolast.mcp.modules.memory.MemoryModule
import com.apptolast.mcp.modules.resources.ResourceModule
import com.apptolast.mcp.server.ServerConfig
import com.apptolast.mcp.server.ToolRegistry
import com.apptolast.mcp.server.registerAllTools
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import com.apptolast.mcp.util.*

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting MCP Full-Stack Server..." }

    val config = ServerConfig.load()

    // Initialize all modules
    logger.info { "Initializing modules..." }
    val filesystemModule = FilesystemModule(config.filesystem)
    val bashExecutor = BashExecutor(config.bash)
    val githubModule = GitHubModule(config.github)
    val memoryModule = MemoryModule(config.memory)
    val postgresModule = PostgreSQLModule(config.database.postgresql)
    val mongoModule = MongoDBModule(config.database.mongodb)
    val resourceModule = ResourceModule(config.resources)

    // Create and configure ToolRegistry
    val toolRegistry = ToolRegistry()
    registerAllTools(
        registry = toolRegistry,
        filesystem = filesystemModule,
        bash = bashExecutor,
        github = githubModule,
        memory = memoryModule,
        postgres = postgresModule,
        mongo = mongoModule,
        resources = resourceModule
    )

    logger.info { "Registered ${toolRegistry.size()} tools in the MCP server" }

    embeddedServer(Netty, port = config.port, host = config.host) {
        configureMcpServer(config, toolRegistry)
    }.start(wait = true)
}

fun Application.configureMcpServer(config: ServerConfig, toolRegistry: ToolRegistry) {
    // Install JSON content negotiation
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    routing {
        get("/") {
            call.respondText("MCP Full-Stack Server - v1.0.0")
        }

        get("/health") {
            call.respondText("OK")
        }

        get("/ready") {
            call.respondText("READY")
        }

        get("/info") {
            val info = mapOf(
                "name" to "mcp-fullstack-server",
                "version" to "1.0.0",
                "capabilities" to mapOf(
                    "filesystem" to "enabled",
                    "bash" to "enabled",
                    "github" to "enabled",
                    "memory" to "enabled",
                    "database" to "enabled",
                    "resources" to "enabled"
                ),
                "tools" to toolRegistry.size()
            )
            call.respond(info)
        }

        // MCP JSON-RPC endpoint
        post("/mcp") {
            var extractedId: JsonElement? = null
            try {
                // Try to extract the id from the raw request body first
                val rawBody = call.receiveText()
                try {
                    val json = Json.parseToJsonElement(rawBody)
                    if (json is JsonObject) {
                        extractedId = json["id"]
                    }
                } catch (e: Exception) {
                    logger.debug(e) { "Failed to pre-parse JSON for id extraction" }
                }
                
                // Now try to deserialize to JsonRpcRequest
                val request = Json.decodeFromString<JsonRpcRequest>(rawBody)
                logger.info { "Received JSON-RPC request: method=${request.method}, id=${request.id}" }

                val response = handleMcpRequest(request, toolRegistry)
                call.respond(response)
            } catch (e: Exception) {
                logger.error(e) { "Failed to handle MCP request" }
                val errorResponse = JsonRpcResponse(
                    id = extractedId,
                    error = JsonRpcError(
                        code = -32603,
                        message = "Internal error: ${e.message}"
                    )
                )
                call.respond(HttpStatusCode.InternalServerError, errorResponse)
            }
        }

        // List all available tools
        get("/tools/list") {
            call.respond(toolRegistry.listTools())
        }
    }

    logger.info { "MCP Server initialized on ${config.host}:${config.port}" }
    logger.info { "Available modules: Filesystem, Bash, GitHub, Memory, Database, Resources" }
    logger.info { "Registered ${toolRegistry.size()} tools" }
}

/**
 * Handles MCP JSON-RPC requests and routes them to the appropriate tool
 */
suspend fun handleMcpRequest(request: JsonRpcRequest, toolRegistry: ToolRegistry): JsonRpcResponse {
    return when (request.method) {
        "tools/list" -> {
            // Return list of available tools
            val tools = toolRegistry.listTools()
            JsonRpcResponse(
                id = request.id,
                result = Json.encodeToJsonElement(serializer<List<ToolDefinition>>(), tools)
            )
        }
        "tools/call" -> {
            // Invoke a specific tool
            val params = request.params ?: return JsonRpcResponse(
                id = request.id,
                error = JsonRpcError(
                    code = -32602,
                    message = "Invalid params: params required for tools/call"
                )
            )

            val toolName = params["name"]?.jsonPrimitive?.content
            if (toolName == null) {
                return JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcError(
                        code = -32602,
                        message = "Invalid params: 'name' field required"
                    )
                )
            }

            val toolParamsElement = params["arguments"]
            val toolParams = when {
                toolParamsElement == null -> return JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcError(
                        code = -32602,
                        message = "Invalid params: 'arguments' field required"
                    )
                )
                toolParamsElement !is JsonObject -> return JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcError(
                        code = -32602,
                        message = "Invalid params: 'arguments' must be an object"
                    )
                )
                else -> toolParamsElement
            }

            val result = toolRegistry.invoke(toolName, toolParams)

            if (result.isError) {
                val message = result.content.firstOrNull()?.let { 
                    when (it) {
                        is TextContent -> it.text
                        else -> "Tool execution failed"
                    }
                } ?: "Tool execution failed"
                
                JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcError(
                        code = -32000,
                        message = message
                    )
                )
            } else {
                JsonRpcResponse(
                    id = request.id,
                    result = Json.encodeToJsonElement(ToolResult.serializer(), result)
                )
            }
        }
        "initialize" -> {
            // MCP initialization
            val serverInfo = ServerInfo(
                name = "mcp-fullstack-server",
                version = "1.0.0"
            )
            val capabilities = ServerCapabilities(
                tools = ToolsCapability(listChanged = false),
                resources = ResourcesCapability(subscribe = false, listChanged = false)
            )

            val response = buildJsonObject {
                put("serverInfo", Json.encodeToJsonElement(ServerInfo.serializer(), serverInfo))
                put("capabilities", Json.encodeToJsonElement(ServerCapabilities.serializer(), capabilities))
            }

            JsonRpcResponse(
                id = request.id,
                result = response
            )
        }
        else -> {
            // Unknown method
            JsonRpcResponse(
                id = request.id,
                error = JsonRpcError(
                    code = -32601,
                    message = "Method not found: ${request.method}"
                )
            )
        }
    }
}
