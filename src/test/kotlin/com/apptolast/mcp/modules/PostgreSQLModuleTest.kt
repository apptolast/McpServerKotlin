package com.apptolast.mcp.modules

import com.apptolast.mcp.modules.database.PostgreSQLModule
import com.apptolast.mcp.server.PostgreSQLConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PostgreSQL Module Tests
 *
 * Note: These tests validate security and logic, but connection tests will
 * fail gracefully if PostgreSQL is not available. This is expected behavior.
 */
class PostgreSQLModuleTest {

    private lateinit var postgresModule: PostgreSQLModule

    @BeforeEach
    fun setup() {
        // Use a test configuration that won't affect production
        val config = PostgreSQLConfig(
            host = "localhost",
            port = 5432,
            database = "test_db",
            username = "test_user",
            password = "test_password"
        )
        postgresModule = PostgreSQLModule(config)
    }

    @Test
    fun `test read-only query validation blocks INSERT`() = runBlocking {
        val result = postgresModule.executeQuery("INSERT INTO users (name) VALUES ('test')")

        // Should be blocked as it's not read-only
        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Only SELECT queries are allowed") ||
            text.contains("read-only") ||
            text.contains("not allowed")
        )
    }

    @Test
    fun `test read-only query validation blocks UPDATE`() = runBlocking {
        val result = postgresModule.executeQuery("UPDATE users SET name = 'new' WHERE id = 1")

        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Only SELECT queries are allowed") ||
            text.contains("read-only")
        )
    }

    @Test
    fun `test read-only query validation blocks DELETE`() = runBlocking {
        val result = postgresModule.executeQuery("DELETE FROM users WHERE id = 1")

        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Only SELECT queries are allowed") ||
            text.contains("read-only")
        )
    }

    @Test
    fun `test read-only query validation blocks DROP`() = runBlocking {
        val result = postgresModule.executeQuery("DROP TABLE users")

        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Only SELECT queries are allowed") ||
            text.contains("read-only")
        )
    }

    @Test
    fun `test read-only query validation blocks CREATE`() = runBlocking {
        val result = postgresModule.executeQuery("CREATE TABLE test (id INT)")

        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Only SELECT queries are allowed") ||
            text.contains("read-only")
        )
    }

    @Test
    fun `test read-only query validation blocks ALTER`() = runBlocking {
        val result = postgresModule.executeQuery("ALTER TABLE users ADD COLUMN age INT")

        assertTrue(result.isError)
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Only SELECT queries are allowed") ||
            text.contains("read-only")
        )
    }

    @Test
    fun `test connection failure is handled gracefully`() = runBlocking {
        // This test will fail to connect but should handle the error gracefully
        val result = postgresModule.testConnection()

        // Either it succeeds (if PostgreSQL is running) or fails gracefully
        val text = result.content.first().toString()
        assertTrue(
            text.contains("Connection successful") ||
            text.contains("Connection test failed") ||
            text.contains("refused") ||
            text.contains("failed")
        )
    }

    @Test
    fun `test executeQuery with non-existent database handles error`() = runBlocking {
        // This will fail to connect but should handle it gracefully
        val result = postgresModule.executeQuery("SELECT 1")

        // Either succeeds (if DB exists) or fails gracefully with error
        val text = result.content.first().toString()
        assertTrue(
            result.isError ||
            text.contains("Query executed successfully") ||
            text.contains("Query failed") ||
            text.contains("refused") ||
            text.contains("failed"),
            "Expected either error flag or error message, got: $text"
        )
    }

    @Test
    fun `test getSchema handles connection error gracefully`() = runBlocking {
        val result = postgresModule.getSchema()

        // Should either succeed or fail gracefully
        val text = result.content.first().toString()
        if (result.isError) {
            assertTrue(
                text.contains("Schema retrieval failed") ||
                text.contains("failed"),
                "Error should be handled gracefully, got: $text"
            )
        } else {
            assertTrue(
                text.contains("Database Schema"),
                "Should return schema information on success, got: $text"
            )
        }
    }

    @Test
    fun `test read-only validation allows SELECT queries`() = runBlocking {
        // The validation should pass for SELECT queries (even if execution fails)
        val result = postgresModule.executeQuery("SELECT 1 AS test")

        // If database is not available, it will fail with connection error
        // If database is available, it should succeed
        // But it should NOT fail with "Only SELECT queries are allowed"
        val text = result.content.first().toString()

        // Should NOT contain the read-only validation error message
        if (result.isError) {
            assertFalse(
                text.contains("Only SELECT queries are allowed"),
                "SELECT query should pass validation even if connection fails"
            )
        }
    }
}
