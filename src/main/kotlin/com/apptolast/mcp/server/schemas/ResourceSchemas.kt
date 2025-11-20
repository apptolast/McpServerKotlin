package com.apptolast.mcp.server.schemas

import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * JSON Schemas for Resource management module
 *
 * Provides 4 tools:
 * - resourcesList: List available resources
 * - resourcesRead: Read resource content
 * - resourcesCreate: Create a new resource
 * - resourcesDelete: Delete a resource
 *
 * Resources are URI-based content (documentation, templates, etc.)
 * that can be accessed by AI clients
 */
object ResourceSchemas {

    /**
     * Schema for resourcesList tool
     * Lists all available resources in the resource directory
     */
    val list = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("pattern") {
                put("type", "string")
                put("description", "Optional filter pattern for resource names")
            }
        },
        required = listOf()  // No required parameters
    )

    /**
     * Schema for resourcesRead tool
     * Reads the content of a specific resource
     */
    val read = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("name") {
                put("type", "string")
                put("description", "Resource name (without resource:// prefix)")
            }
        },
        required = listOf("name")
    )

    /**
     * Schema for resourcesCreate tool
     * Creates a new resource with content
     */
    val create = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("name") {
                put("type", "string")
                put("description", "Resource name (will be prefixed with resource://)")
            }
            putJsonObject("content") {
                put("type", "string")
                put("description", "Resource content (text or base64 for binary)")
            }
            putJsonObject("mimeType") {
                put("type", "string")
                put("description", "MIME type of the resource")
                put("default", "text/plain")
            }
        },
        required = listOf("name", "content")
    )

    /**
     * Schema for resourcesDelete tool
     * Deletes a resource
     */
    val delete = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("name") {
                put("type", "string")
                put("description", "Resource name to delete")
            }
        },
        required = listOf("name")
    )
}
