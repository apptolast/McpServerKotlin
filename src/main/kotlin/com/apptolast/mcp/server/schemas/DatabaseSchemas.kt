package com.apptolast.mcp.server.schemas

import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * JSON Schemas for Database modules (PostgreSQL and MongoDB)
 *
 * PostgreSQL (3 tools):
 * - postgresQuery: Execute read-only SQL queries
 * - postgresGetSchema: Get database schema information
 * - postgresTestConnection: Test database connectivity
 *
 * MongoDB (5 tools):
 * - mongoFind: Find documents in a collection
 * - mongoListCollections: List all collections
 * - mongoCount: Count documents matching a filter
 * - mongoAggregate: Run aggregation pipeline
 * - mongoTestConnection: Test database connectivity
 */
object DatabaseSchemas {

    // ========== PostgreSQL Schemas ==========

    /**
     * Schema for postgresQuery tool
     * Executes read-only SQL queries (SELECT, SHOW, DESCRIBE, EXPLAIN only)
     */
    val postgresQuery = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("sql") {
                put("type", "string")
                put("description", "SQL query to execute (read-only operations only)")
            }
            putJsonObject("params") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "string")
                }
                put("description", "Query parameters for prepared statements (optional)")
            }
            putJsonObject("limit") {
                put("type", "integer")
                put("description", "Maximum number of rows to return")
                put("default", 1000)
                put("minimum", 1)
                put("maximum", 10000)
            }
        },
        required = listOf("sql")
    )

    /**
     * Schema for postgresGetSchema tool
     * Retrieves database schema information (tables, columns, types)
     */
    val postgresGetSchema = Tool.Input(
        properties = buildJsonObject {},  // No properties
        required = listOf()  // No required parameters
    )

    /**
     * Schema for postgresTestConnection tool
     * Tests connectivity to the PostgreSQL database
     */
    val postgresTestConnection = Tool.Input(
        properties = buildJsonObject {},  // No properties
        required = listOf()  // No required parameters
    )

    // ========== MongoDB Schemas ==========

    /**
     * Schema for mongoFind tool
     * Finds documents in a MongoDB collection
     */
    val mongoFind = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("collection") {
                put("type", "string")
                put("description", "Collection name")
            }
            putJsonObject("filter") {
                put("type", "string")
                put("description", "MongoDB filter query as JSON string (e.g., '{\"status\": \"active\"}')")
                put("default", "{}")
            }
            putJsonObject("limit") {
                put("type", "integer")
                put("description", "Maximum number of documents to return")
                put("default", 100)
                put("minimum", 1)
                put("maximum", 10000)
            }
        },
        required = listOf("collection")
    )

    /**
     * Schema for mongoListCollections tool
     * Lists all collections in the MongoDB database
     */
    val mongoListCollections = Tool.Input(
        properties = buildJsonObject {},  // No properties
        required = listOf()  // No required parameters
    )

    /**
     * Schema for mongoCount tool
     * Counts documents matching a filter in a collection
     */
    val mongoCount = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("collection") {
                put("type", "string")
                put("description", "Collection name")
            }
            putJsonObject("filter") {
                put("type", "string")
                put("description", "MongoDB filter query as JSON string")
                put("default", "{}")
            }
        },
        required = listOf("collection")
    )

    /**
     * Schema for mongoAggregate tool
     * Runs an aggregation pipeline on a collection
     */
    val mongoAggregate = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("collection") {
                put("type", "string")
                put("description", "Collection name")
            }
            putJsonObject("pipeline") {
                put("type", "string")
                put("description", "Aggregation pipeline as JSON array string (e.g., '[{\"${'$'}match\": {\"status\": \"active\"}}]')")
            }
        },
        required = listOf("collection", "pipeline")
    )

    /**
     * Schema for mongoTestConnection tool
     * Tests connectivity to the MongoDB database
     */
    val mongoTestConnection = Tool.Input(
        properties = buildJsonObject {},  // No properties
        required = listOf()  // No required parameters
    )
}
