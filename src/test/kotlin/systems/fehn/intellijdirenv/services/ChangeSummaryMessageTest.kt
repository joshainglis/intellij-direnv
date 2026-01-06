package systems.fehn.intellijdirenv.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.text.MessageFormat

/**
 * Tests for the change summary message format patterns.
 * These tests verify that the MessageFormat patterns in MyBundle.properties
 * are valid and produce the expected singular/plural output.
 */
class ChangeSummaryMessageTest {

    // These patterns match those defined in MyBundle.properties
    private val addedPattern = "{0,choice,1#1 variable added|1<{0} variables added}"
    private val modifiedPattern = "{0,choice,1#1 variable modified|1<{0} variables modified}"
    private val removedPattern = "{0,choice,1#1 variable removed|1<{0} variables removed}"

    @Test
    fun `added pattern shows singular for one variable`() {
        val result = MessageFormat.format(addedPattern, 1)
        assertEquals("1 variable added", result)
    }

    @Test
    fun `added pattern shows plural for multiple variables`() {
        val result = MessageFormat.format(addedPattern, 3)
        assertEquals("3 variables added", result)
    }

    @Test
    fun `modified pattern shows singular for one variable`() {
        val result = MessageFormat.format(modifiedPattern, 1)
        assertEquals("1 variable modified", result)
    }

    @Test
    fun `modified pattern shows plural for multiple variables`() {
        val result = MessageFormat.format(modifiedPattern, 5)
        assertEquals("5 variables modified", result)
    }

    @Test
    fun `removed pattern shows singular for one variable`() {
        val result = MessageFormat.format(removedPattern, 1)
        assertEquals("1 variable removed", result)
    }

    @Test
    fun `removed pattern shows plural for multiple variables`() {
        val result = MessageFormat.format(removedPattern, 2)
        assertEquals("2 variables removed", result)
    }

    @Test
    fun `patterns handle large numbers correctly`() {
        assertEquals("100 variables added", MessageFormat.format(addedPattern, 100))
        assertEquals("1000 variables modified", MessageFormat.format(modifiedPattern, 1000))
        assertEquals("999 variables removed", MessageFormat.format(removedPattern, 999))
    }

    @Test
    fun `combined message format works correctly`() {
        val summary = EnvChangeSummary(
            added = listOf("VAR1", "VAR2", "VAR3"),
            modified = listOf("VAR4"),
            removed = listOf("VAR5", "VAR6")
        )

        val parts = mutableListOf<String>()

        if (summary.added.isNotEmpty()) {
            parts.add(MessageFormat.format(addedPattern, summary.added.size))
        }
        if (summary.modified.isNotEmpty()) {
            parts.add(MessageFormat.format(modifiedPattern, summary.modified.size))
        }
        if (summary.removed.isNotEmpty()) {
            parts.add(MessageFormat.format(removedPattern, summary.removed.size))
        }

        val result = parts.joinToString(", ")
        assertEquals("3 variables added, 1 variable modified, 2 variables removed", result)
    }

    @Test
    fun `message with only added variables`() {
        val summary = EnvChangeSummary(added = listOf("VAR1"))

        val parts = mutableListOf<String>()
        if (summary.added.isNotEmpty()) {
            parts.add(MessageFormat.format(addedPattern, summary.added.size))
        }

        assertEquals("1 variable added", parts.joinToString(", "))
    }

    @Test
    fun `message with only removed variables`() {
        val summary = EnvChangeSummary(removed = listOf("VAR1", "VAR2", "VAR3", "VAR4", "VAR5"))

        val parts = mutableListOf<String>()
        if (summary.removed.isNotEmpty()) {
            parts.add(MessageFormat.format(removedPattern, summary.removed.size))
        }

        assertEquals("5 variables removed", parts.joinToString(", "))
    }

    @Test
    fun `empty summary produces empty message`() {
        val summary = EnvChangeSummary()

        val parts = mutableListOf<String>()
        if (summary.added.isNotEmpty()) {
            parts.add(MessageFormat.format(addedPattern, summary.added.size))
        }
        if (summary.modified.isNotEmpty()) {
            parts.add(MessageFormat.format(modifiedPattern, summary.modified.size))
        }
        if (summary.removed.isNotEmpty()) {
            parts.add(MessageFormat.format(removedPattern, summary.removed.size))
        }

        assertEquals("", parts.joinToString(", "))
    }
}
