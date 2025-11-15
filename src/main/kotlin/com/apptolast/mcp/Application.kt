package com.apptolast.mcp

import com.apptolast.mcp.server.ServerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting MCP Full-Stack Server..." }
    
    val config = ServerConfig.load()
    
    embeddedServer(Netty, port = config.port, host = config.host) {
        configureMcpServer(config)
    }.start(wait = true)
}

fun Application.configureMcpServer(config: ServerConfig) {
    routing {
        get("/") {
            call.respondText("MCP Full-Stack Server - v1.0.0")
        }
        
        get("/health") {
            call.respondText("OK")
        }
        
        get("/ready") {
            call.respondText("READY")
        }
        
        get("/info") {
            val info = mapOf(
                "name" to "mcp-fullstack-server",
                "version" to "1.0.0",
                "capabilities" to mapOf(
                    "filesystem" to "enabled",
                    "bash" to "enabled",
                    "github" to "enabled",
                    "memory" to "enabled",
                    "database" to "enabled",
                    "resources" to "enabled"
                )
            )
            call.respondText(
                Json.encodeToString(info),
                io.ktor.http.ContentType.Application.Json
            )
        }
    }
    
    logger.info { "MCP Server initialized on ${config.host}:${config.port}" }
    logger.info { "Available modules: Filesystem, Bash, GitHub, Memory, Database, Resources" }
}
