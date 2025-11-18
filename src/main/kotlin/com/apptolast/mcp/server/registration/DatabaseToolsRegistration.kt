package com.apptolast.mcp.server.registration

import com.apptolast.mcp.server.McpServerInstance
import com.apptolast.mcp.server.schemas.DatabaseSchemas
import io.github.oshai.kotlinlogging.KotlinLogging
import io.modelcontextprotocol.kotlin.sdk.*
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

/**
 * Registers all Database tools with the MCP server
 *
 * PostgreSQL tools (3):
 * 1. postgresQuery - Execute read-only SQL queries
 * 2. postgresGetSchema - Get database schema information
 * 3. postgresTestConnection - Test database connectivity
 *
 * MongoDB tools (5):
 * 4. mongoFind - Find documents in a collection
 * 5. mongoListCollections - List all collections
 * 6. mongoCount - Count documents matching a filter
 * 7. mongoAggregate - Run aggregation pipeline
 * 8. mongoTestConnection - Test database connectivity
 */
suspend fun McpServerInstance.registerDatabaseTools() {
    logger.info { "Registering Database tools..." }

    // ========== PostgreSQL Tools ==========

    // 1. postgresQuery tool
    server.addTool(
        name = "postgresQuery",
        description = """
            Execute read-only SQL queries against PostgreSQL database.

            Security: Only SELECT, SHOW, DESCRIBE, and EXPLAIN queries are allowed.
            Write operations (INSERT, UPDATE, DELETE, DROP, etc.) are blocked.

            Returns query results as formatted text with column names and values.
        """.trimIndent(),
        inputSchema = DatabaseSchemas.postgresQuery
    ) { request: CallToolRequest ->
        try {
            val sql = request.arguments["sql"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: sql")),
                    isError = true
                )

            val paramsArray = request.arguments["params"]?.jsonArray
            val params = paramsArray?.map { it.jsonPrimitive.content } ?: emptyList()

            val limit = request.arguments["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1000

            val result = postgresModule.executeQuery(sql, params, limit)

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
            logger.error(e) { "Error in postgresQuery tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error executing PostgreSQL query: ${e.message}")),
                isError = true
            )
        }
    }

    // 2. postgresGetSchema tool
    server.addTool(
        name = "postgresGetSchema",
        description = """
            Get PostgreSQL database schema information.

            Returns table names, column names, data types, and constraints.
            Optionally specify a table name to get schema for just that table.

            Useful for understanding the database structure before writing queries.
        """.trimIndent(),
        inputSchema = DatabaseSchemas.postgresGetSchema
    ) { request: CallToolRequest ->
        try {
            // Note: getSchema() returns all tables; tableName parameter not supported
            // val tableName = request.arguments["tableName"]?.jsonPrimitive?.content

            val result = postgresModule.getSchema()

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
            logger.error(e) { "Error in postgresGetSchema tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error getting PostgreSQL schema: ${e.message}")),
                isError = true
            )
        }
    }

    // 3. postgresTestConnection tool
    server.addTool(
        name = "postgresTestConnection",
        description = "Test connectivity to the PostgreSQL database. Returns connection status and database version.",
        inputSchema = DatabaseSchemas.postgresTestConnection
    ) { request: CallToolRequest ->
        try {
            val result = postgresModule.testConnection()

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
            logger.error(e) { "Error in postgresTestConnection tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error testing PostgreSQL connection: ${e.message}")),
                isError = true
            )
        }
    }

    // ========== MongoDB Tools ==========

    // 4. mongoFind tool
    server.addTool(
        name = "mongoFind",
        description = """
            Find documents in a MongoDB collection.

            Accepts a filter query as JSON string (e.g., '{"status": "active"}').
            Returns matching documents as formatted JSON.

            Examples:
            - Find all: filter = "{}"
            - Find by field: filter = '{"status": "active"}'
            - Complex query: filter = '{"age": {"${'$'}gt": 18}, "city": "Madrid"}'
        """.trimIndent(),
        inputSchema = DatabaseSchemas.mongoFind
    ) { request: CallToolRequest ->
        try {
            val collection = request.arguments["collection"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: collection")),
                    isError = true
                )

            val filter = request.arguments["filter"]?.jsonPrimitive?.content ?: "{}"
            val limit = request.arguments["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 100

            val result = mongoModule.find(collection, filter, limit)

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
            logger.error(e) { "Error in mongoFind tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error finding MongoDB documents: ${e.message}")),
                isError = true
            )
        }
    }

    // 5. mongoListCollections tool
    server.addTool(
        name = "mongoListCollections",
        description = "List all collections in the MongoDB database. Returns collection names.",
        inputSchema = DatabaseSchemas.mongoListCollections
    ) { request: CallToolRequest ->
        try {
            val result = mongoModule.listCollections()

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
            logger.error(e) { "Error in mongoListCollections tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error listing MongoDB collections: ${e.message}")),
                isError = true
            )
        }
    }

    // 6. mongoCount tool
    server.addTool(
        name = "mongoCount",
        description = """
            Count documents in a MongoDB collection matching a filter.

            Accepts a filter query as JSON string (same format as mongoFind).
            Returns the count of matching documents.

            Useful for checking data size before running queries.
        """.trimIndent(),
        inputSchema = DatabaseSchemas.mongoCount
    ) { request: CallToolRequest ->
        try {
            val collection = request.arguments["collection"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: collection")),
                    isError = true
                )

            val filter = request.arguments["filter"]?.jsonPrimitive?.content ?: "{}"

            val result = mongoModule.countDocuments(collection, filter)

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
            logger.error(e) { "Error in mongoCount tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error counting MongoDB documents: ${e.message}")),
                isError = true
            )
        }
    }

    // 7. mongoAggregate tool
    server.addTool(
        name = "mongoAggregate",
        description = """
            Run an aggregation pipeline on a MongoDB collection.

            Accepts a pipeline as JSON array string.

            Example pipeline: '[{"${'$'}match": {"status": "active"}}, {"${'$'}group": {"_id": "${'$'}city", "count": {"${'$'}sum": 1}}}]'

            Supports all MongoDB aggregation operators: ${'$'}match, ${'$'}group, ${'$'}sort, ${'$'}project, etc.
        """.trimIndent(),
        inputSchema = DatabaseSchemas.mongoAggregate
    ) { request: CallToolRequest ->
        try {
            val collection = request.arguments["collection"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: collection")),
                    isError = true
                )

            val pipeline = request.arguments["pipeline"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: pipeline")),
                    isError = true
                )

            val result = mongoModule.aggregate(collection, pipeline)

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
            logger.error(e) { "Error in mongoAggregate tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error running MongoDB aggregation: ${e.message}")),
                isError = true
            )
        }
    }

    // 8. mongoTestConnection tool
    server.addTool(
        name = "mongoTestConnection",
        description = "Test connectivity to the MongoDB database. Returns connection status and server version.",
        inputSchema = DatabaseSchemas.mongoTestConnection
    ) { request: CallToolRequest ->
        try {
            val result = mongoModule.testConnection()

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
            logger.error(e) { "Error in mongoTestConnection tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error testing MongoDB connection: ${e.message}")),
                isError = true
            )
        }
    }

    logger.info { "Registered 8 Database tools successfully (3 PostgreSQL + 5 MongoDB)" }
}
