package com.apptolast.mcp.server.registration

import com.apptolast.mcp.server.McpServerInstance
import com.apptolast.mcp.server.schemas.BashSchemas
import io.github.oshai.kotlinlogging.KotlinLogging
import io.modelcontextprotocol.kotlin.sdk.*
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

/**
 * Registers Bash execution tool with the MCP server
 *
 * Tool registered:
 * 1. bashExecute - Execute bash commands with security validation
 */
suspend fun McpServerInstance.registerBashTools() {
    logger.info { "Registering Bash tools..." }

    // bashExecute tool
    server.addTool(
        name = "bashExecute",
        description = """
            Execute a bash command with arguments and environment variables.

            Security: Commands are validated against a whitelist and dangerous patterns are blocked.
            Allowed commands include: ls, cat, grep, find, git, npm, gradle, docker, kubectl, curl, and more.

            Dangerous patterns blocked: fork bombs, rm -rf /, sudo, dd, mkfs, chmod 777, etc.
        """.trimIndent(),
        inputSchema = BashSchemas.execute
    ) { request: CallToolRequest ->
        try {
            val command = request.arguments["command"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: command")),
                    isError = true
                )

            // Parse args array
            val argsArray = request.arguments["args"]?.jsonArray
            val args = argsArray?.map { it.jsonPrimitive.content } ?: emptyList()

            // Parse env map
            val envObject = request.arguments["env"]?.jsonObject
            val env = envObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()

            // Note: workingDir is configured at module level, not per-command
            // val workingDir = request.arguments["workingDir"]?.jsonPrimitive?.content

            val result = bashExecutor.execute(command, args, env)

            if (result.isError) {
                CallToolResult(
                    content = listOf(TextContent(text = result.content.joinToString("\n") { (it as? TextContent)?.text ?: "" })),
                    isError = true
                )
            } else {
                CallToolResult(
                    content = result.content.map { TextContent(text = (it as? TextContent)?.text ?: "") },
                    isError = false
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in bashExecute tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error executing bash command: ${e.message}")),
                isError = true
            )
        }
    }

    logger.info { "Registered 1 Bash tool successfully" }
}
