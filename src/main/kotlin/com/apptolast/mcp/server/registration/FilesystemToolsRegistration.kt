package com.apptolast.mcp.server.registration

import com.apptolast.mcp.modules.filesystem.WriteMode
import com.apptolast.mcp.server.McpServerInstance
import com.apptolast.mcp.server.schemas.FilesystemSchemas
import io.github.oshai.kotlinlogging.KotlinLogging
import io.modelcontextprotocol.kotlin.sdk.*
import kotlinx.serialization.json.jsonPrimitive

private val logger = KotlinLogging.logger {}

/**
 * Registers all Filesystem tools with the MCP server
 *
 * Tools registered:
 * 1. readFile - Read file contents
 * 2. writeFile - Write content to file
 * 3. listDirectory - List directory contents
 * 4. createDirectory - Create new directory
 * 5. deleteFile - Delete file or directory
 */
suspend fun McpServerInstance.registerFilesystemTools() {
    logger.info { "Registering Filesystem tools..." }

    // 1. readFile tool
    server.addTool(
        name = "readFile",
        description = "Read the contents of a file from the filesystem. Returns file content as text.",
        inputSchema = FilesystemSchemas.readFile
    ) { request: CallToolRequest ->
        try {
            val path = request.arguments["path"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: path")),
                    isError = true
                )

            val encoding = request.arguments["encoding"]?.jsonPrimitive?.content ?: "UTF-8"

            val result = filesystemModule.readFile(path, encoding)

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
            logger.error(e) { "Error in readFile tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error reading file: ${e.message}")),
                isError = true
            )
        }
    }

    // 2. writeFile tool
    server.addTool(
        name = "writeFile",
        description = "Write content to a file. Supports CREATE (fail if exists), OVERWRITE (replace), or APPEND modes.",
        inputSchema = FilesystemSchemas.writeFile
    ) { request: CallToolRequest ->
        try {
            val path = request.arguments["path"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: path")),
                    isError = true
                )

            val content = request.arguments["content"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: content")),
                    isError = true
                )

            val modeStr = request.arguments["mode"]?.jsonPrimitive?.content ?: "OVERWRITE"
            val mode = WriteMode.valueOf(modeStr)

            // Note: encoding is configured at module level, not per-file
            // val encoding = request.arguments["encoding"]?.jsonPrimitive?.content ?: "UTF-8"

            val result = filesystemModule.writeFile(path, content, mode)

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
            logger.error(e) { "Error in writeFile tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error writing file: ${e.message}")),
                isError = true
            )
        }
    }

    // 3. listDirectory tool
    server.addTool(
        name = "listDirectory",
        description = "List contents of a directory. Can list recursively to show all subdirectories.",
        inputSchema = FilesystemSchemas.listDirectory
    ) { request: CallToolRequest ->
        try {
            val path = request.arguments["path"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: path")),
                    isError = true
                )

            val recursive = request.arguments["recursive"]?.jsonPrimitive?.content?.toBoolean() ?: false
            val maxDepth = request.arguments["maxDepth"]?.jsonPrimitive?.content?.toIntOrNull() ?: 2

            val result = filesystemModule.listDirectory(path, recursive, maxDepth)

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
            logger.error(e) { "Error in listDirectory tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error listing directory: ${e.message}")),
                isError = true
            )
        }
    }

    // 4. createDirectory tool
    server.addTool(
        name = "createDirectory",
        description = "Create a new directory, including parent directories if they don't exist.",
        inputSchema = FilesystemSchemas.createDirectory
    ) { request: CallToolRequest ->
        try {
            val path = request.arguments["path"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: path")),
                    isError = true
                )

            val recursive = request.arguments["recursive"]?.jsonPrimitive?.content?.toBoolean() ?: true

            val result = filesystemModule.createDirectory(path, recursive)

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
            logger.error(e) { "Error in createDirectory tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error creating directory: ${e.message}")),
                isError = true
            )
        }
    }

    // 5. deleteFile tool
    server.addTool(
        name = "deleteFile",
        description = "Delete a file or directory. Use with caution as this operation cannot be undone.",
        inputSchema = FilesystemSchemas.deleteFile
    ) { request: CallToolRequest ->
        try {
            val path = request.arguments["path"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: path")),
                    isError = true
                )

            val recursive = request.arguments["recursive"]?.jsonPrimitive?.content?.toBoolean() ?: false

            val result = filesystemModule.deleteFile(path, recursive)

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
            logger.error(e) { "Error in deleteFile tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error deleting file: ${e.message}")),
                isError = true
            )
        }
    }

    logger.info { "Registered 5 Filesystem tools successfully" }
}
