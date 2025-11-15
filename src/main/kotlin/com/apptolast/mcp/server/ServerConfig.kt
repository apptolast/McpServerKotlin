package com.apptolast.mcp.server

import com.typesafe.config.ConfigFactory
import java.nio.file.Path
import java.nio.file.Paths

data class ServerConfig(
    val host: String,
    val port: Int,
    val filesystem: FilesystemConfig,
    val bash: BashConfig,
    val github: GitHubConfig,
    val memory: MemoryConfig,
    val database: DatabaseConfig,
    val resources: ResourcesConfig
) {
    companion object {
        fun load(): ServerConfig {
            val config = ConfigFactory.load()
            
            return ServerConfig(
                host = config.getString("server.host"),
                port = config.getInt("server.port"),
                filesystem = FilesystemConfig(
                    allowedDirectories = config.getStringList("filesystem.allowedDirectories")
                        .map { Paths.get(it) },
                    maxFileSize = config.getLong("filesystem.maxFileSize"),
                    allowedExtensions = config.getStringList("filesystem.allowedExtensions").toSet()
                ),
                bash = BashConfig(
                    allowedCommands = config.getStringList("bash.allowedCommands").toSet(),
                    workingDirectory = Paths.get(config.getString("bash.workingDirectory")),
                    timeoutSeconds = config.getLong("bash.timeoutSeconds")
                ),
                github = GitHubConfig(
                    repoPath = Paths.get(config.getString("github.repoPath")),
                    token = config.getString("github.token").takeIf { it.isNotBlank() }
                ),
                memory = MemoryConfig(
                    storagePath = Paths.get(config.getString("memory.storagePath"))
                ),
                database = DatabaseConfig(
                    postgresql = PostgreSQLConfig(
                        host = config.getString("database.postgresql.host"),
                        port = config.getInt("database.postgresql.port"),
                        database = config.getString("database.postgresql.database"),
                        username = config.getString("database.postgresql.username"),
                        password = config.getString("database.postgresql.password")
                    ),
                    mongodb = MongoDBConfig(
                        connectionString = config.getString("database.mongodb.connectionString"),
                        database = config.getString("database.mongodb.database")
                    )
                ),
                resources = ResourcesConfig(
                    resourcesPath = Paths.get(config.getString("resources.path"))
                )
            )
        }
    }
}

data class FilesystemConfig(
    val allowedDirectories: List<Path>,
    val maxFileSize: Long,
    val allowedExtensions: Set<String>
)

data class BashConfig(
    val allowedCommands: Set<String>,
    val workingDirectory: Path,
    val timeoutSeconds: Long
)

data class GitHubConfig(
    val repoPath: Path,
    val token: String?
)

data class MemoryConfig(
    val storagePath: Path
)

data class DatabaseConfig(
    val postgresql: PostgreSQLConfig,
    val mongodb: MongoDBConfig
)

data class PostgreSQLConfig(
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String
)

data class MongoDBConfig(
    val connectionString: String,
    val database: String
)

data class ResourcesConfig(
    val resourcesPath: Path
)
