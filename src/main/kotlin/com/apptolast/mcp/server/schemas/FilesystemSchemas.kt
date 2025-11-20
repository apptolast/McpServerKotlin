package com.apptolast.mcp.server.schemas

import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * JSON Schemas for Filesystem module tools
 *
 * Provides 5 tools:
 * - readFile: Read file contents
 * - writeFile: Write data to file
 * - listDirectory: List directory contents
 * - createDirectory: Create new directory
 * - deleteFile: Delete file or directory
 */
object FilesystemSchemas {

    /**
     * Schema for readFile tool
     * Reads the contents of a file from the filesystem
     */
    val readFile = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Absolute or relative path to the file to read")
            }
            putJsonObject("encoding") {
                put("type", "string")
                put("description", "Character encoding for reading the file")
                put("default", "UTF-8")
            }
        },
        required = listOf("path")
    )

    /**
     * Schema for writeFile tool
     * Writes content to a file, with support for different write modes
     */
    val writeFile = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Path to the file to write")
            }
            putJsonObject("content") {
                put("type", "string")
                put("description", "Content to write to the file")
            }
            putJsonObject("mode") {
                put("type", "string")
                put("description", "Write mode: CREATE (fail if exists), OVERWRITE (replace), or APPEND (add to end)")
                putJsonArray("enum") {
                    add("CREATE")
                    add("OVERWRITE")
                    add("APPEND")
                }
                put("default", "OVERWRITE")
            }
        },
        required = listOf("path", "content")
    )

    /**
     * Schema for listDirectory tool
     * Lists contents of a directory
     */
    val listDirectory = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Path to the directory to list")
            }
            putJsonObject("recursive") {
                put("type", "boolean")
                put("description", "Whether to list subdirectories recursively")
                put("default", false)
            }
            putJsonObject("maxDepth") {
                put("type", "integer")
                put("description", "Maximum depth for recursive listing (only applies when recursive=true)")
                put("default", 2)
                put("minimum", 1)
                put("maximum", 10)
            }
        },
        required = listOf("path")
    )

    /**
     * Schema for createDirectory tool
     * Creates a new directory (with parent directories if needed)
     */
    val createDirectory = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Path to the directory to create")
            }
            putJsonObject("recursive") {
                put("type", "boolean")
                put("description", "Create parent directories if they don't exist")
                put("default", true)
            }
        },
        required = listOf("path")
    )

    /**
     * Schema for deleteFile tool
     * Deletes a file or directory
     */
    val deleteFile = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Path to the file or directory to delete")
            }
            putJsonObject("recursive") {
                put("type", "boolean")
                put("description", "Recursively delete directory contents (use with caution!)")
                put("default", false)
            }
        },
        required = listOf("path")
    )
}
