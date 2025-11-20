package com.apptolast.mcp.server.schemas

import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * JSON Schemas for Knowledge Graph Memory module
 *
 * Provides 4 tools:
 * - createEntities: Create entities in the knowledge graph
 * - createRelations: Create relations between entities
 * - searchNodes: Search for nodes by name
 * - openNodes: Retrieve full node details
 */
object MemorySchemas {

    /**
     * Schema for createEntities tool
     * Creates new entities in the knowledge graph
     */
    val createEntities = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("entities") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("name") {
                            put("type", "string")
                            put("description", "Unique entity name")
                        }
                        putJsonObject("entityType") {
                            put("type", "string")
                            put("description", "Type of entity (e.g., 'project', 'file', 'class', 'function')")
                        }
                        putJsonObject("observations") {
                            put("type", "array")
                            putJsonObject("items") {
                                put("type", "string")
                            }
                            put("description", "List of observations about this entity")
                        }
                    }
                    putJsonArray("required") {
                        add("name")
                        add("entityType")
                    }
                }
                put("description", "Array of entities to create")
            }
        },
        required = listOf("entities")
    )

    /**
     * Schema for createRelations tool
     * Creates relations between existing entities
     */
    val createRelations = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("relations") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("from") {
                            put("type", "string")
                            put("description", "Source entity name")
                        }
                        putJsonObject("to") {
                            put("type", "string")
                            put("description", "Target entity name")
                        }
                        putJsonObject("relationType") {
                            put("type", "string")
                            put("description", "Type of relation (e.g., 'uses', 'dependsOn', 'implements', 'extends')")
                        }
                    }
                    putJsonArray("required") {
                        add("from")
                        add("to")
                        add("relationType")
                    }
                }
                put("description", "Array of relations to create")
            }
        },
        required = listOf("relations")
    )

    /**
     * Schema for searchNodes tool
     * Searches for nodes in the knowledge graph by name
     */
    val searchNodes = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("query") {
                put("type", "string")
                put("description", "Search query (entity name or pattern)")
            }
        },
        required = listOf("query")
    )

    /**
     * Schema for openNodes tool
     * Retrieves full details of specific nodes
     */
    val openNodes = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("names") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "string")
                }
                put("description", "Array of entity names to retrieve")
            }
        },
        required = listOf("names")
    )
}
