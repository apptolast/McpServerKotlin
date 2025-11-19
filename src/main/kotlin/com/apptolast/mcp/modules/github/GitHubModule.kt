package com.apptolast.mcp.modules.github

import com.apptolast.mcp.server.GitHubConfig
import com.apptolast.mcp.util.ToolResult
import com.apptolast.mcp.util.TextContent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.nio.file.Files

private val logger = KotlinLogging.logger {}

class GitHubModule(
    private val config: GitHubConfig
) {
    
    private fun getOrInitGit(): Git {
        return if (Files.exists(config.repoPath.resolve(".git"))) {
            Git.open(config.repoPath.toFile())
        } else {
            Files.createDirectories(config.repoPath)
            Git.init().setDirectory(config.repoPath.toFile()).call()
        }
    }
    
    suspend fun status(): ToolResult = withContext(Dispatchers.IO) {
        try {
            val git = getOrInitGit()
            val status = git.status().call()
            
            val statusInfo = buildString {
                val branch = git.repository.branch ?: "main"
                appendLine("Branch: $branch")
                
                if (status.modified.isNotEmpty()) {
                    appendLine("\nModified files:")
                    status.modified.forEach { appendLine("  M $it") }
                }
                
                if (status.untracked.isNotEmpty()) {
                    appendLine("\nUntracked files:")
                    status.untracked.forEach { appendLine("  ? $it") }
                }
                
                if (status.added.isNotEmpty()) {
                    appendLine("\nStaged files:")
                    status.added.forEach { appendLine("  A $it") }
                }
                
                if (status.removed.isNotEmpty()) {
                    appendLine("\nRemoved files:")
                    status.removed.forEach { appendLine("  D $it") }
                }
                
                if (status.isClean) {
                    appendLine("\nWorking tree clean")
                }
            }
            
            logger.info { "Git status retrieved" }
            
            ToolResult(
                content = listOf(TextContent(text = statusInfo))
            )
        } catch (e: Exception) {
            logger.error(e) { "Git status failed" }
            ToolResult.error("Git status failed: ${e.message}")
        }
    }
    
    suspend fun commit(
        message: String,
        files: List<String>? = emptyList(),
        author: String = "MCP Server",
        email: String = "mcp@apptolast.com"
    ): ToolResult = withContext(Dispatchers.IO) {
        try {
            val git = getOrInitGit()
            
            // Stage files only if files parameter is not null
            if (files != null) {
                val addCommand = git.add()
                if (files.isEmpty()) {
                    addCommand.addFilepattern(".")
                } else {
                    files.forEach { addCommand.addFilepattern(it) }
                }
                addCommand.call()
            }
            
            // Commit
            val commit = git.commit()
                .setMessage(message)
                .setAuthor(PersonIdent(author, email))
                .call()
            
            val shortId = commit.id.abbreviate(7).name()
            logger.info { "Committed: $shortId - $message" }
            
            ToolResult(
                content = listOf(
                    TextContent(
                        text = "Committed: $shortId - $message\nAuthor: $author <$email>"
                    )
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Git commit failed" }
            ToolResult.error("Git commit failed: ${e.message}")
        }
    }
    
    suspend fun push(
        remote: String = "origin",
        branch: String? = null,
        force: Boolean = false
    ): ToolResult = withContext(Dispatchers.IO) {
        try {
            val git = getOrInitGit()
            val pushCommand = git.push()
                .setRemote(remote)
                .setForce(force)
            
            if (branch != null) {
                pushCommand.add(branch)
            }
            
            if (config.token != null) {
                pushCommand.setCredentialsProvider(
                    UsernamePasswordCredentialsProvider(config.token, "")
                )
            }
            
            val results = pushCommand.call()
            
            val resultText = buildString {
                appendLine("Pushed to $remote${branch?.let { "/$it" } ?: ""}")
                results.forEach { result ->
                    result.remoteUpdates.forEach { update ->
                        appendLine("  ${update.remoteName}: ${update.status}")
                    }
                }
            }
            
            logger.info { "Pushed to $remote" }
            
            ToolResult(
                content = listOf(TextContent(text = resultText))
            )
        } catch (e: Exception) {
            logger.error(e) { "Git push failed" }
            ToolResult.error("Git push failed: ${e.message}")
        }
    }
    
    suspend fun clone(
        url: String,
        targetPath: String? = null
    ): ToolResult = withContext(Dispatchers.IO) {
        try {
            val clonePath = if (targetPath != null) {
                config.repoPath.resolve(targetPath)
            } else {
                config.repoPath
            }
            
            Files.createDirectories(clonePath.parent)
            
            val cloneCommand = Git.cloneRepository()
                .setURI(url)
                .setDirectory(clonePath.toFile())
            
            if (config.token != null) {
                cloneCommand.setCredentialsProvider(
                    UsernamePasswordCredentialsProvider(config.token, "")
                )
            }
            
            val git = cloneCommand.call()
            val repoName = git.repository.directory.parent
            
            logger.info { "Cloned repository from $url" }
            
            ToolResult(
                content = listOf(
                    TextContent(text = "Successfully cloned repository to: $repoName")
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Git clone failed" }
            ToolResult.error("Git clone failed: ${e.message}")
        }
    }
    
    suspend fun log(maxCount: Int = 10): ToolResult = withContext(Dispatchers.IO) {
        try {
            val git = getOrInitGit()
            val logs = git.log()
                .setMaxCount(maxCount)
                .call()
            
            val logText = buildString {
                appendLine("Commit history (last $maxCount):\n")
                logs.forEach { commit ->
                    val shortId = commit.id.abbreviate(7).name()
                    val author = commit.authorIdent.name
                    val date = commit.authorIdent.`when`
                    val message = commit.shortMessage
                    
                    appendLine("commit $shortId")
                    appendLine("Author: $author")
                    appendLine("Date:   $date")
                    appendLine("    $message")
                    appendLine()
                }
            }
            
            logger.info { "Retrieved git log" }
            
            ToolResult(
                content = listOf(TextContent(text = logText))
            )
        } catch (e: Exception) {
            logger.error(e) { "Git log failed" }
            ToolResult.error("Git log failed: ${e.message}")
        }
    }
    
    suspend fun branch(
        name: String? = null,
        checkout: Boolean = false
    ): ToolResult = withContext(Dispatchers.IO) {
        try {
            val git = getOrInitGit()
            
            if (name == null) {
                // List branches
                val branches = git.branchList().call()
                val branchText = buildString {
                    appendLine("Branches:")
                    branches.forEach { ref ->
                        val branchName = ref.name.removePrefix("refs/heads/")
                        val current = if (git.repository.branch == branchName) "*" else " "
                        appendLine("  $current $branchName")
                    }
                }
                
                ToolResult(
                    content = listOf(TextContent(text = branchText))
                )
            } else {
                // Create or checkout branch
                if (checkout) {
                    git.checkout().setName(name).setCreateBranch(true).call()
                    ToolResult(
                        content = listOf(
                            TextContent(text = "Switched to new branch '$name'")
                        )
                    )
                } else {
                    git.branchCreate().setName(name).call()
                    ToolResult(
                        content = listOf(
                            TextContent(text = "Created branch '$name'")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Git branch operation failed" }
            ToolResult.error("Git branch operation failed: ${e.message}")
        }
    }
}
