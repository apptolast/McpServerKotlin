package com.apptolast.mcp.modules.memory

import com.apptolast.mcp.server.MemoryConfig
import com.apptolast.mcp.util.ToolResult
import com.apptolast.mcp.util.TextContent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.readLines

private val logger = KotlinLogging.logger {}

@Serializable
data class Entity(
    val name: String,
    val entityType: String,
    val observations: List<String> = emptyList(),
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString()
)

@Serializable
data class Relation(
    val from: String,
    val to: String,
    val relationType: String,
    val createdAt: String = Clock.System.now().toString()
)

@Serializable
data class KnowledgeGraph(
    val entities: Map<String, Entity> = emptyMap(),
    val relations: List<Relation> = emptyList()
)

class MemoryModule(
    private val config: MemoryConfig
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val graphFile: Path = config.storagePath.resolve("knowledge_graph.jsonl")
    
    init {
        Files.createDirectories(config.storagePath)
    }
    
    suspend fun createEntities(entities: List<EntityInput>): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                val graph = loadGraph()
                val newEntities = entities.map { input ->
                    Entity(
                        name = input.name,
                        entityType = input.entityType,
                        observations = input.observations
                    )
                }
                
                val updatedGraph = graph.copy(
                    entities = graph.entities + newEntities.associateBy { it.name }
                )
                
                saveGraph(updatedGraph)
                
                logger.info { "Created ${newEntities.size} entities" }
                
                ToolResult(
                    content = listOf(
                        TextContent(
                            text = "Created ${newEntities.size} entities: ${newEntities.joinToString { it.name }}"
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to create entities" }
                ToolResult.error("Failed to create entities: ${e.message}")
            }
        }
    }
    
    suspend fun createRelations(relations: List<RelationInput>): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                val graph = loadGraph()
                val newRelations = relations.map { input ->
                    Relation(
                        from = input.from,
                        to = input.to,
                        relationType = input.relationType
                    )
                }
                
                val updatedGraph = graph.copy(
                    relations = graph.relations + newRelations
                )
                
                saveGraph(updatedGraph)
                
                logger.info { "Created ${newRelations.size} relations" }
                
                ToolResult(
                    content = listOf(
                        TextContent(
                            text = "Created ${newRelations.size} relations"
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to create relations" }
                ToolResult.error("Failed to create relations: ${e.message}")
            }
        }
    }
    
    suspend fun searchNodes(query: String): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                val graph = loadGraph()
                
                // Simple search by name and observations
                val matchingEntities = graph.entities.values.filter { entity ->
                    entity.name.contains(query, ignoreCase = true) ||
                    entity.observations.any { it.contains(query, ignoreCase = true) }
                }
                
                // Get relevant relations
                val relevantRelations = graph.relations.filter { relation ->
                    matchingEntities.any { it.name == relation.from || it.name == relation.to }
                }
                
                val result = buildString {
                    appendLine("Found ${matchingEntities.size} matching entities:")
                    matchingEntities.forEach { entity ->
                        appendLine("\n- ${entity.name} (${entity.entityType})")
                        entity.observations.forEach { obs ->
                            appendLine("  * $obs")
                        }
                    }
                    
                    if (relevantRelations.isNotEmpty()) {
                        appendLine("\nRelevant relations (${relevantRelations.size}):")
                        relevantRelations.forEach { rel ->
                            appendLine("  ${rel.from} --[${rel.relationType}]--> ${rel.to}")
                        }
                    }
                }
                
                logger.info { "Search query '$query' found ${matchingEntities.size} entities" }
                
                ToolResult(
                    content = listOf(TextContent(text = result))
                )
            } catch (e: Exception) {
                logger.error(e) { "Search failed" }
                ToolResult.error("Search failed: ${e.message}")
            }
        }
    }
    
    suspend fun openNodes(names: List<String>): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                val graph = loadGraph()
                
                val entities = names.mapNotNull { name ->
                    graph.entities[name]
                }
                
                if (entities.isEmpty()) {
                    return@withContext ToolResult.error("No entities found with the specified names")
                }
                
                val result = buildString {
                    appendLine("Found ${entities.size} entities:")
                    entities.forEach { entity ->
                        appendLine("\n${entity.name} (${entity.entityType})")
                        appendLine("Created: ${entity.createdAt}")
                        appendLine("Observations:")
                        entity.observations.forEach { obs ->
                            appendLine("  - $obs")
                        }
                    }
                }
                
                logger.info { "Opened ${entities.size} entities" }
                
                ToolResult(
                    content = listOf(TextContent(text = result))
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to open nodes" }
                ToolResult.error("Failed to open nodes: ${e.message}")
            }
        }
    }
    
    private fun loadGraph(): KnowledgeGraph {
        if (!Files.exists(graphFile)) {
            return KnowledgeGraph()
        }
        
        val lines = graphFile.readLines()
        val entities = mutableMapOf<String, Entity>()
        val relations = mutableListOf<Relation>()
        
        lines.forEach { line ->
            if (line.isBlank()) return@forEach
            
            try {
                val obj = json.parseToJsonElement(line).jsonObject
                when (obj["type"]?.jsonPrimitive?.content) {
                    "entity" -> {
                        val entity = json.decodeFromJsonElement<Entity>(obj)
                        entities[entity.name] = entity
                    }
                    "relation" -> {
                        val relation = json.decodeFromJsonElement<Relation>(obj)
                        relations.add(relation)
                    }
                }
            } catch (e: Exception) {
                logger.warn { "Failed to parse line: $line" }
            }
        }
        
        return KnowledgeGraph(entities, relations)
    }
    
    private fun saveGraph(graph: KnowledgeGraph) {
        Files.createDirectories(graphFile.parent)
        
        graphFile.bufferedWriter().use { writer ->
            // Write entities
            graph.entities.values.forEach { entity ->
                val obj = json.encodeToJsonElement(entity).jsonObject.toMutableMap()
                obj["type"] = JsonPrimitive("entity")
                writer.write(json.encodeToString(JsonObject(obj)))
                writer.newLine()
            }
            
            // Write relations
            graph.relations.forEach { relation ->
                val obj = json.encodeToJsonElement(relation).jsonObject.toMutableMap()
                obj["type"] = JsonPrimitive("relation")
                writer.write(json.encodeToString(JsonObject(obj)))
                writer.newLine()
            }
        }
    }
}

@Serializable
data class EntityInput(
    val name: String,
    val entityType: String,
    val observations: List<String>
)

@Serializable
data class RelationInput(
    val from: String,
    val to: String,
    val relationType: String
)
