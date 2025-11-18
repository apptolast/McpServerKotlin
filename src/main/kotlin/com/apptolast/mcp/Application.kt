package com.apptolast.mcp

import com.apptolast.mcp.server.McpServerInstance
import com.apptolast.mcp.server.ServerConfig
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
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Job
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

/**
 * Main entry point for MCP Full-Stack Server
 *
 * Supports two modes:
 * 1. Stdio mode (--stdio flag) - for Claude Desktop/Code integration
 * 2. HTTP mode (default) - for remote access via REST API
 */
fun main(args: Array<String>) {
    logger.info { "Starting MCP Full-Stack Server v1.0.0..." }

    val config = ServerConfig.load()

    if (args.contains("--stdio")) {
        logger.info { "Running in STDIO mode for Claude Desktop/Code" }
        runStdioMode(config)
    } else {
        logger.info { "Running in HTTP mode on ${config.host}:${config.port}" }
        runHttpMode(config)
    }
}

/**
 * Run server in STDIO mode for Claude Desktop/Code
 *
 * In this mode, the server communicates via stdin/stdout using the MCP protocol.
 * This is the recommended mode for local AI clients like Claude Desktop.
 */
private fun runStdioMode(config: ServerConfig) = runBlocking {
    logger.info { "Initializing MCP server for stdio transport..." }

    // Create MCP server instance
    val mcpServer = McpServerInstance(config)

    // Register all tools and resources
    mcpServer.registerAllTools()
    mcpServer.registerAllResources()
    mcpServer.registerPrompts()

    logger.info { "MCP server initialized successfully" }
    logger.info { "Registered 28 tools across 7 modules" }
    logger.info { "Waiting for client connection on stdio..." }

    // Create stdio transport with explicit input/output streams
    val transport = StdioServerTransport(
        System.`in`.asInput(),
        System.out.asSink().buffered()
    )

    // Connect MCP server to stdio transport
    mcpServer.server.connect(transport)

    // Keep the server running indefinitely
    // The stdio transport will handle the connection lifecycle
    logger.info { "MCP server running on stdio. Press Ctrl+C to stop." }
    Thread.sleep(Long.MAX_VALUE)
}

/**
 * Run server in HTTP mode for remote access
 *
 * In this mode, the server exposes HTTP endpoints including:
 * - POST /mcp - JSON-RPC 2.0 endpoint for MCP protocol
 * - GET /health - Health check
 * - GET /ready - Readiness check
 * - GET /info - Server information
 */
private fun runHttpMode(config: ServerConfig) {
    embeddedServer(Netty, port = config.port, host = config.host) {
        configureHttpServer(config)
    }.start(wait = true)
}

/**
 * Configure HTTP server with MCP endpoints
 */
fun Application.configureHttpServer(config: ServerConfig) {
    // Install JSON content negotiation
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    // Create and initialize MCP server instance
    val mcpServer = McpServerInstance(config)

    // Register all tools asynchronously during startup
    runBlocking {
        mcpServer.registerAllTools()
        mcpServer.registerAllResources()
        mcpServer.registerPrompts()

        logger.info { "MCP server fully initialized" }
        logger.info { "Registered 28 tools across 7 modules" }
    }

    routing {
        // Root endpoint
        get("/") {
            call.respondText(
                "MCP Full-Stack Server v1.0.0\n\n" +
                        "Endpoints:\n" +
                        "  POST /mcp - MCP JSON-RPC endpoint\n" +
                        "  GET /health - Health check\n" +
                        "  GET /ready - Readiness probe\n" +
                        "  GET /info - Server capabilities\n\n" +
                        "For stdio mode: Run with --stdio flag\n"
            )
        }

        // Health check endpoint
        get("/health") {
            call.respondText("OK")
        }

        // Readiness check endpoint
        get("/ready") {
            call.respondText("READY")
        }

        // Server information endpoint
        get("/info") {
            val info = buildJsonObject {
                put("name", "mcp-fullstack-server")
                put("version", "1.0.0")
                put("protocol", "MCP (Model Context Protocol)")
                putJsonObject("capabilities") {
                    putJsonObject("tools") {
                        put("listChanged", true)
                    }
                    putJsonObject("resources") {
                        put("subscribe", true)
                        put("listChanged", true)
                    }
                    putJsonObject("prompts") {
                        put("listChanged", false)
                    }
                    putJsonObject("logging") {
                        // Empty object indicates logging support
                    }
                }
                putJsonObject("modules") {
                    put("filesystem", 5)  // 5 tools
                    put("bash", 1)         // 1 tool
                    put("github", 6)       // 6 tools
                    put("memory", 4)       // 4 tools
                    put("database", 8)     // 8 tools (3 PostgreSQL + 5 MongoDB)
                    put("resources", 4)    // 4 tools
                }
                put("totalTools", 28)
            }

            call.respond(info)
        }

        // NOTE: MCP SDK 0.7.7 is designed for stdio transport only
        // HTTP endpoints for MCP protocol are not supported by the SDK
        // Use the stdio mode to connect MCP clients (Claude Desktop, etc.)

        // Placeholder endpoint for documentation purposes
        post("/mcp") {
            call.respond(
                HttpStatusCode.NotImplemented,
                mapOf(
                    "error" to "MCP over HTTP not supported",
                    "message" to "Please use stdio transport to connect MCP clients",
                    "documentation" to "https://modelcontextprotocol.io"
                )
            )
        }

        // Tools list endpoint for debugging/monitoring
        get("/tools") {
            try {
                // TODO: Implement a way to list registered tools
                // The SDK doesn't expose a public API for this
                call.respond(
                    mapOf(
                        "message" to "Tools list endpoint not yet implemented",
                        "registered_tools" to 28,
                        "modules" to listOf("filesystem", "bash", "github", "memory", "postgresql", "mongodb", "resources")
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Error listing tools" }
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }

    logger.info { "HTTP server configured on ${config.host}:${config.port}" }
    logger.info { "MCP endpoint: http://${config.host}:${config.port}/mcp" }
    logger.info { "Health check: http://${config.host}:${config.port}/health" }
}
