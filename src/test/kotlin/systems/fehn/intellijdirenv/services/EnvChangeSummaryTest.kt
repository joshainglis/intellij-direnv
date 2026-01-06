package systems.fehn.intellijdirenv.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EnvChangeSummaryTest {

    @Test
    fun `hasChanges returns false when all lists are empty`() {
        val summary = EnvChangeSummary()
        assertFalse(summary.hasChanges)
    }

    @Test
    fun `hasChanges returns true when added list is not empty`() {
        val summary = EnvChangeSummary(added = listOf("VAR1"))
        assertTrue(summary.hasChanges)
    }

    @Test
    fun `hasChanges returns true when modified list is not empty`() {
        val summary = EnvChangeSummary(modified = listOf("VAR1"))
        assertTrue(summary.hasChanges)
    }

    @Test
    fun `hasChanges returns true when removed list is not empty`() {
        val summary = EnvChangeSummary(removed = listOf("VAR1"))
        assertTrue(summary.hasChanges)
    }

    @Test
    fun `hasChanges returns true when multiple lists have items`() {
        val summary = EnvChangeSummary(
            added = listOf("VAR1", "VAR2"),
            modified = listOf("VAR3"),
            removed = listOf("VAR4", "VAR5", "VAR6")
        )
        assertTrue(summary.hasChanges)
    }

    @Test
    fun `default constructor creates empty lists`() {
        val summary = EnvChangeSummary()
        assertTrue(summary.added.isEmpty())
        assertTrue(summary.modified.isEmpty())
        assertTrue(summary.removed.isEmpty())
    }

    @Test
    fun `constructor preserves variable names`() {
        val added = listOf("NEW_VAR", "ANOTHER_VAR")
        val modified = listOf("CHANGED_VAR")
        val removed = listOf("OLD_VAR")

        val summary = EnvChangeSummary(added, modified, removed)

        assertEquals(added, summary.added)
        assertEquals(modified, summary.modified)
        assertEquals(removed, summary.removed)
    }

    @Test
    fun `data class equality works correctly`() {
        val summary1 = EnvChangeSummary(
            added = listOf("VAR1"),
            modified = listOf("VAR2"),
            removed = listOf("VAR3")
        )
        val summary2 = EnvChangeSummary(
            added = listOf("VAR1"),
            modified = listOf("VAR2"),
            removed = listOf("VAR3")
        )

        assertEquals(summary1, summary2)
    }

    @Test
    fun `data class inequality works correctly`() {
        val summary1 = EnvChangeSummary(added = listOf("VAR1"))
        val summary2 = EnvChangeSummary(added = listOf("VAR2"))

        assertNotEquals(summary1, summary2)
    }
}
