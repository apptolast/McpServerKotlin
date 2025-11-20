package com.apptolast.mcp.server.registration

import com.apptolast.mcp.server.McpServerInstance
import com.apptolast.mcp.server.schemas.ResourceSchemas
import io.github.oshai.kotlinlogging.KotlinLogging
import io.modelcontextprotocol.kotlin.sdk.*
import kotlinx.serialization.json.jsonPrimitive

private val logger = KotlinLogging.logger {}

/**
 * Registers all Resource management tools with the MCP server
 *
 * Tools registered:
 * 1. resourcesList - List available resources
 * 2. resourcesRead - Read resource content
 * 3. resourcesCreate - Create a new resource
 * 4. resourcesDelete - Delete a resource
 *
 * Resources are URI-based content (documentation, templates, configs)
 * that provide context and information to AI clients.
 */
suspend fun McpServerInstance.registerResourceTools() {
    logger.info { "Registering Resource management tools..." }

    // 1. resourcesList tool
    server.addTool(
        name = "resourcesList",
        description = """
            List all available resources in the resource directory.

            Resources are documentation files, templates, configuration examples, and other
            reference materials that provide context for the AI.

            Returns resource names, URIs, descriptions, and MIME types.

            Optionally filter by pattern to search for specific resources.
        """.trimIndent(),
        inputSchema = ResourceSchemas.list
    ) { request: CallToolRequest ->
        try {
            val result = resourceModule.listResources()

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
            logger.error(e) { "Error in resourcesList tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error listing resources: ${e.message}")),
                isError = true
            )
        }
    }

    // 2. resourcesRead tool
    server.addTool(
        name = "resourcesRead",
        description = """
            Read the content of a specific resource.

            Resources provide documentation, examples, and reference materials.

            Returns:
            - Text resources: Full content as text
            - Binary resources: Base64-encoded content
            - Includes MIME type information

            Use resourcesList first to discover available resources.
        """.trimIndent(),
        inputSchema = ResourceSchemas.read
    ) { request: CallToolRequest ->
        try {
            val name = request.arguments["name"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: name")),
                    isError = true
                )

            val result = resourceModule.readResource(name)

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
            logger.error(e) { "Error in resourcesRead tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error reading resource: ${e.message}")),
                isError = true
            )
        }
    }

    // 3. resourcesCreate tool
    server.addTool(
        name = "resourcesCreate",
        description = """
            Create a new resource with content.

            Resources are stored in the resource directory and can be accessed by:
            - AI clients for reference and documentation
            - Other tools for templates and configurations
            - Future sessions for persistent knowledge

            Specify:
            - name: Resource name (e.g., "api-docs.md", "config-template.yaml")
            - content: Resource content (text or base64 for binary)
            - mimeType: MIME type (default: text/plain)

            Created resources are immediately available via resourcesList and resourcesRead.
        """.trimIndent(),
        inputSchema = ResourceSchemas.create
    ) { request: CallToolRequest ->
        try {
            val name = request.arguments["name"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: name")),
                    isError = true
                )

            val content = request.arguments["content"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: content")),
                    isError = true
                )

            val mimeType = request.arguments["mimeType"]?.jsonPrimitive?.content ?: "text/plain"

            val result = resourceModule.createResource(name, content, mimeType)

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
            logger.error(e) { "Error in resourcesCreate tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error creating resource: ${e.message}")),
                isError = true
            )
        }
    }

    // 4. resourcesDelete tool
    server.addTool(
        name = "resourcesDelete",
        description = """
            Delete a resource from the resource directory.

            Use with caution as this operation cannot be undone.

            Removes the resource file permanently. The resource will no longer
            appear in resourcesList and cannot be read via resourcesRead.

            To temporarily hide a resource, consider moving it to a different location
            using filesystem tools instead of deleting it.
        """.trimIndent(),
        inputSchema = ResourceSchemas.delete
    ) { request: CallToolRequest ->
        try {
            val name = request.arguments["name"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: name")),
                    isError = true
                )

            val result = resourceModule.deleteResource(name)

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
            logger.error(e) { "Error in resourcesDelete tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error deleting resource: ${e.message}")),
                isError = true
            )
        }
    }

    logger.info { "Registered 4 Resource management tools successfully" }
}
