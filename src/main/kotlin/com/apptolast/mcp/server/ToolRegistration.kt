package com.apptolast.mcp.server

import com.apptolast.mcp.modules.bash.BashExecutor
import com.apptolast.mcp.modules.database.MongoDBModule
import com.apptolast.mcp.modules.database.PostgreSQLModule
import com.apptolast.mcp.modules.filesystem.FilesystemModule
import com.apptolast.mcp.modules.filesystem.WriteMode
import com.apptolast.mcp.modules.github.GitHubModule
import com.apptolast.mcp.modules.memory.EntityInput
import com.apptolast.mcp.modules.memory.MemoryModule
import com.apptolast.mcp.modules.memory.RelationInput
import com.apptolast.mcp.modules.resources.ResourceModule
import kotlinx.serialization.json.*

/**
 * Registers all available tools from all modules into the ToolRegistry.
 *
 * This function is responsible for:
 * - Defining the input schema for each tool
 * - Creating handler functions that map JSON params to module method calls
 * - Registering each tool with the ToolRegistry
 */
fun registerAllTools(
    registry: ToolRegistry,
    filesystem: FilesystemModule,
    bash: BashExecutor,
    github: GitHubModule,
    memory: MemoryModule,
    postgres: PostgreSQLModule,
    mongo: MongoDBModule,
    resources: ResourceModule
) {
    // ========== FILESYSTEM TOOLS ==========
    registerFilesystemTools(registry, filesystem)

    // ========== BASH TOOLS ==========
    registerBashTools(registry, bash)

    // ========== GITHUB TOOLS ==========
    registerGitHubTools(registry, github)

    // ========== MEMORY TOOLS ==========
    registerMemoryTools(registry, memory)

    // ========== DATABASE TOOLS (PostgreSQL) ==========
    registerPostgreSQLTools(registry, postgres)

    // ========== DATABASE TOOLS (MongoDB) ==========
    registerMongoDBTools(registry, mongo)

    // ========== RESOURCE TOOLS ==========
    registerResourceTools(registry, resources)
}

// ==================== FILESYSTEM TOOLS ====================
private fun registerFilesystemTools(registry: ToolRegistry, filesystem: FilesystemModule) {
    registry.register(
        name = "fs_read",
        description = "Read the contents of a file from the filesystem",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "path" to SchemaBuilder.stringProperty("Absolute or relative path to the file"),
                "encoding" to SchemaBuilder.stringProperty("File encoding (default: UTF-8)", listOf("UTF-8", "ISO-8859-1", "US-ASCII"))
            ),
            required = listOf("path")
        ),
        handler = { params ->
            val path = params["path"]?.jsonPrimitive?.content ?: ""
            val encoding = params["encoding"]?.jsonPrimitive?.contentOrNull ?: "UTF-8"
            filesystem.readFile(path, encoding)
        }
    )

    registry.register(
        name = "fs_write",
        description = "Write content to a file in the filesystem",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "path" to SchemaBuilder.stringProperty("Path to the file"),
                "content" to SchemaBuilder.stringProperty("Content to write"),
                "mode" to SchemaBuilder.stringProperty(
                    "Write mode: CREATE (fail if exists), OVERWRITE (replace if exists), APPEND (add to end)",
                    listOf("CREATE", "OVERWRITE", "APPEND")
                )
            ),
            required = listOf("path", "content")
        ),
        handler = { params ->
            val path = params["path"]?.jsonPrimitive?.content ?: ""
            val content = params["content"]?.jsonPrimitive?.content ?: ""
            val modeStr = params["mode"]?.jsonPrimitive?.contentOrNull ?: "CREATE"
            val mode = WriteMode.valueOf(modeStr)
            filesystem.writeFile(path, content, mode)
        }
    )

    registry.register(
        name = "fs_list",
        description = "List files and directories in a directory",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "path" to SchemaBuilder.stringProperty("Path to the directory"),
                "recursive" to SchemaBuilder.booleanProperty("List recursively (default: false)", false),
                "maxDepth" to SchemaBuilder.integerProperty("Maximum recursion depth (default: 2)", 1, 10)
            ),
            required = listOf("path")
        ),
        handler = { params ->
            val path = params["path"]?.jsonPrimitive?.content ?: ""
            val recursive = params["recursive"]?.jsonPrimitive?.booleanOrNull ?: false
            val maxDepth = params["maxDepth"]?.jsonPrimitive?.intOrNull ?: 2
            filesystem.listDirectory(path, recursive, maxDepth)
        }
    )

    registry.register(
        name = "fs_create_dir",
        description = "Create a new directory in the filesystem",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "path" to SchemaBuilder.stringProperty("Path to the directory to create"),
                "recursive" to SchemaBuilder.booleanProperty("Create parent directories if they don't exist (default: true)", true)
            ),
            required = listOf("path")
        ),
        handler = { params ->
            val path = params["path"]?.jsonPrimitive?.content ?: ""
            val recursive = params["recursive"]?.jsonPrimitive?.booleanOrNull ?: true
            filesystem.createDirectory(path, recursive)
        }
    )

    registry.register(
        name = "fs_delete",
        description = "Delete a file or directory from the filesystem",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "path" to SchemaBuilder.stringProperty("Path to the file or directory to delete"),
                "recursive" to SchemaBuilder.booleanProperty("Delete directories recursively (default: false)", false)
            ),
            required = listOf("path")
        ),
        handler = { params ->
            val path = params["path"]?.jsonPrimitive?.content ?: ""
            val recursive = params["recursive"]?.jsonPrimitive?.booleanOrNull ?: false
            filesystem.deleteFile(path, recursive)
        }
    )
}

// ==================== BASH TOOLS ====================
private fun registerBashTools(registry: ToolRegistry, bash: BashExecutor) {
    registry.register(
        name = "bash_execute",
        description = "Execute a bash command in a controlled environment. Only whitelisted commands are allowed.",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "command" to SchemaBuilder.stringProperty("The base command to execute (must be in allowed list)"),
                "args" to SchemaBuilder.arrayProperty(
                    "Command arguments (optional)",
                    SchemaBuilder.stringProperty("Argument")
                ),
                "env" to SchemaBuilder.objectProperty(
                    "Environment variables to set (optional)",
                    emptyMap()
                )
            ),
            required = listOf("command")
        ),
        handler = { params ->
            val command = params["command"]?.jsonPrimitive?.content ?: ""
            val args = params["args"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val env = params["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
            bash.execute(command, args, env)
        }
    )
}

// ==================== GITHUB TOOLS ====================
private fun registerGitHubTools(registry: ToolRegistry, github: GitHubModule) {
    registry.register(
        name = "git_status",
        description = "Get the status of the Git repository (modified, untracked, staged files)",
        inputSchema = SchemaBuilder.createSchema(
            properties = emptyMap(),
            required = emptyList()
        ),
        handler = { _ ->
            github.status()
        }
    )

    registry.register(
        name = "git_commit",
        description = "Commit changes to the Git repository",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "message" to SchemaBuilder.stringProperty("Commit message"),
                "files" to SchemaBuilder.arrayProperty(
                    "Files to stage (empty = stage all)",
                    SchemaBuilder.stringProperty("File path")
                ),
                "author" to SchemaBuilder.stringProperty("Author name (optional)", null),
                "email" to SchemaBuilder.stringProperty("Author email (optional)", null)
            ),
            required = listOf("message")
        ),
        handler = { params ->
            val message = params["message"]?.jsonPrimitive?.content ?: ""
            val files = params["files"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val author = params["author"]?.jsonPrimitive?.contentOrNull ?: "MCP Server"
            val email = params["email"]?.jsonPrimitive?.contentOrNull ?: "mcp@apptolast.com"
            github.commit(message, files, author, email)
        }
    )

    registry.register(
        name = "git_push",
        description = "Push commits to a remote Git repository",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "remote" to SchemaBuilder.stringProperty("Remote name (default: origin)", null),
                "branch" to SchemaBuilder.stringProperty("Branch to push (default: current branch)", null)
            ),
            required = emptyList()
        ),
        handler = { params ->
            val remote = params["remote"]?.jsonPrimitive?.contentOrNull ?: "origin"
            val branch = params["branch"]?.jsonPrimitive?.contentOrNull ?: ""
            github.push(remote, branch)
        }
    )

    registry.register(
        name = "git_clone",
        description = "Clone a Git repository from a URL",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "url" to SchemaBuilder.stringProperty("Git repository URL"),
                "branch" to SchemaBuilder.stringProperty("Branch to clone (optional)", null)
            ),
            required = listOf("url")
        ),
        handler = { params ->
            val url = params["url"]?.jsonPrimitive?.content ?: ""
            val branch = params["branch"]?.jsonPrimitive?.contentOrNull
            github.clone(url, branch)
        }
    )

    registry.register(
        name = "git_log",
        description = "Show commit history",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "maxCount" to SchemaBuilder.integerProperty("Maximum number of commits to show (default: 10)", 1, 100)
            ),
            required = emptyList()
        ),
        handler = { params ->
            val maxCount = params["maxCount"]?.jsonPrimitive?.intOrNull ?: 10
            github.log(maxCount)
        }
    )

    registry.register(
        name = "git_branch",
        description = "List or checkout Git branches",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "name" to SchemaBuilder.stringProperty("Branch name (null to list all branches)", null),
                "checkout" to SchemaBuilder.booleanProperty("Checkout the branch if true (default: false)", false)
            ),
            required = emptyList()
        ),
        handler = { params ->
            val name = params["name"]?.jsonPrimitive?.contentOrNull
            val checkout = params["checkout"]?.jsonPrimitive?.booleanOrNull ?: false
            github.branch(name, checkout)
        }
    )
}

// ==================== MEMORY TOOLS ====================
private fun registerMemoryTools(registry: ToolRegistry, memory: MemoryModule) {
    registry.register(
        name = "memory_create_entities",
        description = "Create entities in the knowledge graph memory",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "entities" to SchemaBuilder.arrayProperty(
                    "List of entities to create",
                    SchemaBuilder.objectProperty(
                        "Entity object",
                        mapOf(
                            "name" to SchemaBuilder.stringProperty("Entity name"),
                            "entityType" to SchemaBuilder.stringProperty("Entity type/category"),
                            "observations" to SchemaBuilder.arrayProperty(
                                "List of observations about this entity",
                                SchemaBuilder.stringProperty("Observation")
                            )
                        )
                    )
                )
            ),
            required = listOf("entities")
        ),
        handler = { params ->
            val entitiesJson = params["entities"]?.jsonArray ?: JsonArray(emptyList())
            val entities = entitiesJson.map { entityObj ->
                val obj = entityObj.jsonObject
                EntityInput(
                    name = obj["name"]?.jsonPrimitive?.content ?: "",
                    entityType = obj["entityType"]?.jsonPrimitive?.content ?: "",
                    observations = obj["observations"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                )
            }
            memory.createEntities(entities)
        }
    )

    registry.register(
        name = "memory_create_relations",
        description = "Create relations between entities in the knowledge graph",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "relations" to SchemaBuilder.arrayProperty(
                    "List of relations to create",
                    SchemaBuilder.objectProperty(
                        "Relation object",
                        mapOf(
                            "from" to SchemaBuilder.stringProperty("Source entity name"),
                            "to" to SchemaBuilder.stringProperty("Target entity name"),
                            "relationType" to SchemaBuilder.stringProperty("Type of relation")
                        )
                    )
                )
            ),
            required = listOf("relations")
        ),
        handler = { params ->
            val relationsJson = params["relations"]?.jsonArray ?: JsonArray(emptyList())
            val relations = relationsJson.map { relationObj ->
                val obj = relationObj.jsonObject
                RelationInput(
                    from = obj["from"]?.jsonPrimitive?.content ?: "",
                    to = obj["to"]?.jsonPrimitive?.content ?: "",
                    relationType = obj["relationType"]?.jsonPrimitive?.content ?: ""
                )
            }
            memory.createRelations(relations)
        }
    )

    registry.register(
        name = "memory_search",
        description = "Search for entities and relations in the knowledge graph by query",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "query" to SchemaBuilder.stringProperty("Search query (searches in entity names and observations)")
            ),
            required = listOf("query")
        ),
        handler = { params ->
            val query = params["query"]?.jsonPrimitive?.content ?: ""
            memory.searchNodes(query)
        }
    )

    registry.register(
        name = "memory_open_nodes",
        description = "Retrieve specific entities by their names",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "names" to SchemaBuilder.arrayProperty(
                    "List of entity names to retrieve",
                    SchemaBuilder.stringProperty("Entity name")
                )
            ),
            required = listOf("names")
        ),
        handler = { params ->
            val names = params["names"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            memory.openNodes(names)
        }
    )
}

// ==================== POSTGRESQL TOOLS ====================
private fun registerPostgreSQLTools(registry: ToolRegistry, postgres: PostgreSQLModule) {
    registry.register(
        name = "postgres_query",
        description = "Execute a read-only SQL query on PostgreSQL database (SELECT only)",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "sql" to SchemaBuilder.stringProperty("SQL query (only SELECT queries allowed)"),
                "params" to SchemaBuilder.arrayProperty(
                    "Query parameters for prepared statement (optional)",
                    SchemaBuilder.stringProperty("Parameter value")
                ),
                "maxRows" to SchemaBuilder.integerProperty("Maximum rows to return (default: 1000)", 1, 10000)
            ),
            required = listOf("sql")
        ),
        handler = { params ->
            val sql = params["sql"]?.jsonPrimitive?.content ?: ""
            val paramsList = params["params"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val maxRows = params["maxRows"]?.jsonPrimitive?.intOrNull ?: 1000
            postgres.executeQuery(sql, paramsList, maxRows)
        }
    )

    registry.register(
        name = "postgres_schema",
        description = "Get the database schema (tables and columns) from PostgreSQL",
        inputSchema = SchemaBuilder.createSchema(
            properties = emptyMap(),
            required = emptyList()
        ),
        handler = { _ ->
            postgres.getSchema()
        }
    )

    registry.register(
        name = "postgres_test",
        description = "Test the connection to PostgreSQL database",
        inputSchema = SchemaBuilder.createSchema(
            properties = emptyMap(),
            required = emptyList()
        ),
        handler = { _ ->
            postgres.testConnection()
        }
    )
}

// ==================== MONGODB TOOLS ====================
private fun registerMongoDBTools(registry: ToolRegistry, mongo: MongoDBModule) {
    registry.register(
        name = "mongo_find",
        description = "Find documents in a MongoDB collection",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "collection" to SchemaBuilder.stringProperty("Collection name"),
                "filter" to SchemaBuilder.stringProperty("MongoDB filter query (JSON string, default: '{}')", null),
                "limit" to SchemaBuilder.integerProperty("Maximum documents to return (default: 100)", 1, 1000)
            ),
            required = listOf("collection")
        ),
        handler = { params ->
            val collection = params["collection"]?.jsonPrimitive?.content ?: ""
            val filter = params["filter"]?.jsonPrimitive?.contentOrNull ?: "{}"
            val limit = params["limit"]?.jsonPrimitive?.intOrNull ?: 100
            mongo.find(collection, filter, limit)
        }
    )

    registry.register(
        name = "mongo_list_collections",
        description = "List all collections in the MongoDB database",
        inputSchema = SchemaBuilder.createSchema(
            properties = emptyMap(),
            required = emptyList()
        ),
        handler = { _ ->
            mongo.listCollections()
        }
    )

    registry.register(
        name = "mongo_count",
        description = "Count documents in a MongoDB collection matching a filter",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "collection" to SchemaBuilder.stringProperty("Collection name"),
                "filter" to SchemaBuilder.stringProperty("MongoDB filter query (JSON string, default: '{}')", null)
            ),
            required = listOf("collection")
        ),
        handler = { params ->
            val collection = params["collection"]?.jsonPrimitive?.content ?: ""
            val filter = params["filter"]?.jsonPrimitive?.contentOrNull ?: "{}"
            mongo.countDocuments(collection, filter)
        }
    )

    registry.register(
        name = "mongo_aggregate",
        description = "Execute an aggregation pipeline on a MongoDB collection",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "collection" to SchemaBuilder.stringProperty("Collection name"),
                "pipeline" to SchemaBuilder.stringProperty("Aggregation pipeline (JSON array string)")
            ),
            required = listOf("collection", "pipeline")
        ),
        handler = { params ->
            val collection = params["collection"]?.jsonPrimitive?.content ?: ""
            val pipeline = params["pipeline"]?.jsonPrimitive?.content ?: "[]"
            mongo.aggregate(collection, pipeline)
        }
    )

    registry.register(
        name = "mongo_test",
        description = "Test the connection to MongoDB database",
        inputSchema = SchemaBuilder.createSchema(
            properties = emptyMap(),
            required = emptyList()
        ),
        handler = { _ ->
            mongo.testConnection()
        }
    )
}

// ==================== RESOURCE TOOLS ====================
private fun registerResourceTools(registry: ToolRegistry, resources: ResourceModule) {
    registry.register(
        name = "resource_list",
        description = "List all available resources",
        inputSchema = SchemaBuilder.createSchema(
            properties = emptyMap(),
            required = emptyList()
        ),
        handler = { _ ->
            resources.listResources()
        }
    )

    registry.register(
        name = "resource_read",
        description = "Read a resource by its URI",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "uri" to SchemaBuilder.stringProperty("Resource URI (format: resource://path/to/resource)")
            ),
            required = listOf("uri")
        ),
        handler = { params ->
            val uri = params["uri"]?.jsonPrimitive?.content ?: ""
            resources.readResource(uri)
        }
    )

    registry.register(
        name = "resource_create",
        description = "Create a new resource",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "name" to SchemaBuilder.stringProperty("Resource name"),
                "content" to SchemaBuilder.stringProperty("Resource content"),
                "mimeType" to SchemaBuilder.stringProperty("MIME type (default: text/plain)", null)
            ),
            required = listOf("name", "content")
        ),
        handler = { params ->
            val name = params["name"]?.jsonPrimitive?.content ?: ""
            val content = params["content"]?.jsonPrimitive?.content ?: ""
            val mimeType = params["mimeType"]?.jsonPrimitive?.contentOrNull ?: "text/plain"
            resources.createResource(name, content, mimeType)
        }
    )

    registry.register(
        name = "resource_delete",
        description = "Delete a resource by its URI",
        inputSchema = SchemaBuilder.createSchema(
            properties = mapOf(
                "uri" to SchemaBuilder.stringProperty("Resource URI (format: resource://path/to/resource)")
            ),
            required = listOf("uri")
        ),
        handler = { params ->
            val uri = params["uri"]?.jsonPrimitive?.content ?: ""
            resources.deleteResource(uri)
        }
    )
}
