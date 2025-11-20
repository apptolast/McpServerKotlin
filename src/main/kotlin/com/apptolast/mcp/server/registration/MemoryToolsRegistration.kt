package com.apptolast.mcp.server.registration

import com.apptolast.mcp.modules.memory.EntityInput
import com.apptolast.mcp.modules.memory.RelationInput
import com.apptolast.mcp.server.McpServerInstance
import com.apptolast.mcp.server.schemas.MemorySchemas
import io.github.oshai.kotlinlogging.KotlinLogging
import io.modelcontextprotocol.kotlin.sdk.*
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

/**
 * Registers all Knowledge Graph Memory tools with the MCP server
 *
 * Tools registered:
 * 1. createEntities - Create entities in the knowledge graph
 * 2. createRelations - Create relations between entities
 * 3. searchNodes - Search for nodes by name
 * 4. openNodes - Retrieve full node details
 */
suspend fun McpServerInstance.registerMemoryTools() {
    logger.info { "Registering Memory/Knowledge Graph tools..." }

    // 1. createEntities tool
    server.addTool(
        name = "createEntities",
        description = """
            Create new entities in the knowledge graph.

            Entities represent concepts, files, classes, functions, or any other important elements in the codebase.
            Each entity has a name, type, and optional observations (notes about that entity).

            Example entity types: "project", "file", "class", "function", "module", "api_endpoint", "database_table"
        """.trimIndent(),
        inputSchema = MemorySchemas.createEntities
    ) { request: CallToolRequest ->
        try {
            val entitiesArray = request.arguments["entities"]?.jsonArray
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: entities")),
                    isError = true
                )

            val entities = entitiesArray.map { entityJson ->
                val obj = entityJson.jsonObject
                EntityInput(
                    name = obj["name"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Entity missing 'name' field"),
                    entityType = obj["entityType"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Entity missing 'entityType' field"),
                    observations = obj["observations"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                )
            }

            val result = memoryModule.createEntities(entities)

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
            logger.error(e) { "Error in createEntities tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error creating entities: ${e.message}")),
                isError = true
            )
        }
    }

    // 2. createRelations tool
    server.addTool(
        name = "createRelations",
        description = """
            Create relations between existing entities in the knowledge graph.

            Relations describe how entities are connected. For example:
            - FileA "imports" FileB
            - ClassX "extends" ClassY
            - FunctionZ "uses" ModuleW

            Common relation types: "uses", "dependsOn", "implements", "extends", "calls", "contains", "imports"
        """.trimIndent(),
        inputSchema = MemorySchemas.createRelations
    ) { request: CallToolRequest ->
        try {
            val relationsArray = request.arguments["relations"]?.jsonArray
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: relations")),
                    isError = true
                )

            val relations = relationsArray.map { relationJson ->
                val obj = relationJson.jsonObject
                RelationInput(
                    from = obj["from"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Relation missing 'from' field"),
                    to = obj["to"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Relation missing 'to' field"),
                    relationType = obj["relationType"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Relation missing 'relationType' field")
                )
            }

            val result = memoryModule.createRelations(relations)

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
            logger.error(e) { "Error in createRelations tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error creating relations: ${e.message}")),
                isError = true
            )
        }
    }

    // 3. searchNodes tool
    server.addTool(
        name = "searchNodes",
        description = """
            Search for nodes in the knowledge graph by name.

            Performs fuzzy/partial matching on entity names.
            Returns a list of matching entities with their types and observations.

            Use this to find entities when you don't remember the exact name.
        """.trimIndent(),
        inputSchema = MemorySchemas.searchNodes
    ) { request: CallToolRequest ->
        try {
            val query = request.arguments["query"]?.jsonPrimitive?.content
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: query")),
                    isError = true
                )

            val result = memoryModule.searchNodes(query)

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
            logger.error(e) { "Error in searchNodes tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error searching nodes: ${e.message}")),
                isError = true
            )
        }
    }

    // 4. openNodes tool
    server.addTool(
        name = "openNodes",
        description = """
            Retrieve full details of specific nodes by their exact names.

            Returns complete information about each entity including:
            - Name and type
            - All observations
            - Creation and update timestamps
            - Related entities (both incoming and outgoing relations)

            Use this after searchNodes to get full details about found entities.
        """.trimIndent(),
        inputSchema = MemorySchemas.openNodes
    ) { request: CallToolRequest ->
        try {
            val namesArray = request.arguments["names"]?.jsonArray
                ?: return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Missing required parameter: names")),
                    isError = true
                )

            val names = namesArray.map { it.jsonPrimitive.content }

            val result = memoryModule.openNodes(names)

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
            logger.error(e) { "Error in openNodes tool" }
            CallToolResult(
                content = listOf(TextContent(text = "Error opening nodes: ${e.message}")),
                isError = true
            )
        }
    }

    logger.info { "Registered 4 Memory/Knowledge Graph tools successfully" }
}
