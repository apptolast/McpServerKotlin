package com.apptolast.mcp.modules.database

import com.apptolast.mcp.server.PostgreSQLConfig
import com.apptolast.mcp.util.ToolResult
import com.apptolast.mcp.util.TextContent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.postgresql.ds.PGSimpleDataSource
import java.sql.ResultSet

private val logger = KotlinLogging.logger {}

@Serializable
data class SchemaInfo(
    val schema: String,
    val table: String,
    val columns: List<ColumnInfo>
)

@Serializable
data class ColumnInfo(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val defaultValue: String?
)

class PostgreSQLModule(
    private val config: PostgreSQLConfig
) {
    
    private val dataSource by lazy {
        PGSimpleDataSource().apply {
            serverNames = arrayOf(config.host)
            portNumbers = intArrayOf(config.port)
            databaseName = config.database
            user = config.username
            password = config.password
        }
    }
    
    private fun isReadOnlyQuery(sql: String): Boolean {
        val normalized = sql.trim().uppercase()
        val readOnlyPatterns = listOf("SELECT", "SHOW", "DESCRIBE", "EXPLAIN", "WITH")
        val writePatterns = listOf("INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE")
        
        return readOnlyPatterns.any { normalized.startsWith(it) } &&
               writePatterns.none { normalized.contains(it) }
    }
    
    suspend fun executeQuery(
        sql: String,
        params: List<Any> = emptyList(),
        maxRows: Int = 1000
    ): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate it's a read-only query
                if (!isReadOnlyQuery(sql)) {
                    return@withContext ToolResult.error(
                        "Only SELECT queries are allowed for safety. Found potentially modifying SQL."
                    )
                }
                
                dataSource.connection.use { conn ->
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.maxRows = maxRows
                        
                        // Bind parameters
                        params.forEachIndexed { index, param ->
                            stmt.setObject(index + 1, param)
                        }
                        
                        val rs = stmt.executeQuery()
                        val metadata = rs.metaData
                        val columnCount = metadata.columnCount
                        
                        val columns = (1..columnCount).map { i ->
                            metadata.getColumnName(i) to metadata.getColumnTypeName(i)
                        }
                        
                        val rows = mutableListOf<Map<String, Any?>>()
                        while (rs.next() && rows.size < maxRows) {
                            val row = columns.associate { (colName, _) ->
                                colName to rs.getObject(colName)
                            }
                            rows.add(row)
                        }
                        
                        val result = buildString {
                            appendLine("Query executed successfully")
                            appendLine("Columns: ${columns.map { it.first }.joinToString(", ")}")
                            appendLine("Rows returned: ${rows.size}")
                            appendLine()
                            
                            if (rows.isNotEmpty()) {
                                // Header
                                appendLine(columns.map { it.first }.joinToString(" | "))
                                appendLine(columns.map { "-".repeat(it.first.length) }.joinToString("-+-"))
                                
                                // Rows
                                rows.forEach { row ->
                                    appendLine(columns.map { row[it.first]?.toString() ?: "NULL" }.joinToString(" | "))
                                }
                            }
                        }
                        
                        logger.info { "PostgreSQL query executed: ${rows.size} rows" }
                        
                        ToolResult(
                            content = listOf(TextContent(text = result))
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "PostgreSQL query failed" }
                ToolResult.error("Query failed: ${e.message}")
            }
        }
    }
    
    suspend fun getSchema(): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { conn ->
                    val metadata = conn.metaData
                    val schemas = mutableListOf<SchemaInfo>()
                    
                    val rs = metadata.getTables(null, null, "%", arrayOf("TABLE"))
                    while (rs.next()) {
                        val tableName = rs.getString("TABLE_NAME")
                        val schemaName = rs.getString("TABLE_SCHEM") ?: "public"
                        
                        val columnsRs = metadata.getColumns(null, schemaName, tableName, "%")
                        val columns = mutableListOf<ColumnInfo>()
                        
                        while (columnsRs.next()) {
                            columns.add(
                                ColumnInfo(
                                    name = columnsRs.getString("COLUMN_NAME"),
                                    type = columnsRs.getString("TYPE_NAME"),
                                    nullable = columnsRs.getInt("NULLABLE") == 1,
                                    defaultValue = columnsRs.getString("COLUMN_DEF")
                                )
                            )
                        }
                        
                        schemas.add(
                            SchemaInfo(
                                schema = schemaName,
                                table = tableName,
                                columns = columns
                            )
                        )
                    }
                    
                    val result = buildString {
                        appendLine("Database Schema (${schemas.size} tables):\n")
                        
                        schemas.forEach { schema ->
                            appendLine("Table: ${schema.schema}.${schema.table}")
                            appendLine("Columns:")
                            schema.columns.forEach { col ->
                                val nullable = if (col.nullable) "NULL" else "NOT NULL"
                                val default = col.defaultValue?.let { " DEFAULT $it" } ?: ""
                                appendLine("  - ${col.name}: ${col.type} $nullable$default")
                            }
                            appendLine()
                        }
                    }
                    
                    logger.info { "Retrieved schema for ${schemas.size} tables" }
                    
                    ToolResult(
                        content = listOf(TextContent(text = result))
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "Schema retrieval failed" }
                ToolResult.error("Schema retrieval failed: ${e.message}")
            }
        }
    }
    
    suspend fun testConnection(): ToolResult {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { conn ->
                    val isValid = conn.isValid(5)
                    
                    if (isValid) {
                        val dbVersion = conn.metaData.databaseProductVersion
                        
                        ToolResult(
                            content = listOf(
                                TextContent(
                                    text = "✅ Connection successful\nPostgreSQL version: $dbVersion"
                                )
                            )
                        )
                    } else {
                        ToolResult.error("Connection validation failed")
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Connection test failed" }
                ToolResult.error("Connection test failed: ${e.message}")
            }
        }
    }
}
