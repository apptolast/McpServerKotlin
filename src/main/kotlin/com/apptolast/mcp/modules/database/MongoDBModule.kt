package com.apptolast.mcp.modules.database

import com.apptolast.mcp.server.MongoDBConfig
import com.apptolast.mcp.util.ToolResult
import com.apptolast.mcp.util.TextContent
import io.github.oshai.kotlinlogging.KotlinLogging
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.toList
import org.bson.Document

private val logger = KotlinLogging.logger {}

class MongoDBModule(
    private val config: MongoDBConfig
) {
    
    private val client by lazy {
        MongoClient.create(config.connectionString)
    }
    
    private val database by lazy {
        client.getDatabase(config.database)
    }
    
    suspend fun find(
        collection: String,
        filter: String = "{}",
        limit: Int = 100,
        sort: String? = null
    ): ToolResult {
        return try {
            val coll = database.getCollection<Document>(collection)
            val filterDoc = Document.parse(filter)
            
            var query = coll.find(filterDoc).limit(limit)
            
            if (sort != null) {
                val sortDoc = Document.parse(sort)
                query = query.sort(sortDoc)
            }
            
            val documents = query.toList()
            
            val result = buildString {
                appendLine("Collection: $collection")
                appendLine("Filter: $filter")
                if (sort != null) appendLine("Sort: $sort")
                appendLine("Documents found: ${documents.size}")
                appendLine()
                
                documents.forEachIndexed { index, doc ->
                    appendLine("Document ${index + 1}:")
                    appendLine(doc.toJson())
                    appendLine()
                }
            }
            
            logger.info { "MongoDB find: $collection (${documents.size} documents)" }
            
            ToolResult(
                content = listOf(TextContent(text = result))
            )
        } catch (e: Exception) {
            logger.error(e) { "MongoDB find failed" }
            ToolResult.error("MongoDB find failed: ${e.message}")
        }
    }
    
    suspend fun listCollections(): ToolResult {
        return try {
            val collections = database.listCollectionNames().toList()
            
            val result = buildString {
                appendLine("Database: ${config.database}")
                appendLine("Collections (${collections.size}):")
                collections.forEach { appendLine("  - $it") }
            }
            
            logger.info { "Listed ${collections.size} collections" }
            
            ToolResult(
                content = listOf(TextContent(text = result))
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to list collections" }
            ToolResult.error("Failed to list collections: ${e.message}")
        }
    }
    
    suspend fun countDocuments(
        collection: String,
        filter: String = "{}"
    ): ToolResult {
        return try {
            val coll = database.getCollection<Document>(collection)
            val filterDoc = Document.parse(filter)
            
            val count = coll.countDocuments(filterDoc)
            
            val result = "Collection: $collection\nFilter: $filter\nDocument count: $count"
            
            logger.info { "MongoDB count: $collection ($count documents)" }
            
            ToolResult(
                content = listOf(TextContent(text = result))
            )
        } catch (e: Exception) {
            logger.error(e) { "MongoDB count failed" }
            ToolResult.error("MongoDB count failed: ${e.message}")
        }
    }
    
    suspend fun aggregate(
        collection: String,
        pipeline: String
    ): ToolResult {
        return try {
            val coll = database.getCollection<Document>(collection)
            val pipelineList = Document.parse(pipeline)
                .getList("pipeline", Document::class.java)
            
            val results = coll.aggregate(pipelineList).toList()
            
            val result = buildString {
                appendLine("Collection: $collection")
                appendLine("Pipeline: $pipeline")
                appendLine("Results: ${results.size}")
                appendLine()
                
                results.forEachIndexed { index, doc ->
                    appendLine("Result ${index + 1}:")
                    appendLine(doc.toJson())
                    appendLine()
                }
            }
            
            logger.info { "MongoDB aggregate: $collection (${results.size} results)" }
            
            ToolResult(
                content = listOf(TextContent(text = result))
            )
        } catch (e: Exception) {
            logger.error(e) { "MongoDB aggregate failed" }
            ToolResult.error("MongoDB aggregate failed: ${e.message}")
        }
    }
    
    suspend fun testConnection(): ToolResult {
        return try {
            // Try to list databases as a connection test
            val adminDb = client.getDatabase("admin")
            adminDb.runCommand(Document("ping", 1))
            
            ToolResult(
                content = listOf(
                    TextContent(text = "✅ Connection successful\nDatabase: ${config.database}")
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Connection test failed" }
            ToolResult.error("Connection test failed: ${e.message}")
        }
    }
    
    fun close() {
        client.close()
    }
}
