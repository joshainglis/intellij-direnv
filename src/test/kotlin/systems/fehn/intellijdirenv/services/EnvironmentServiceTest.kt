package systems.fehn.intellijdirenv.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for EnvironmentService functionality.
 * Note: These tests modify the actual JVM environment, so they should be run in isolation.
 */
class EnvironmentServiceTest {

    private val envService = EnvironmentService()

    @Test
    fun `getVariable returns null for non-existent variable`() {
        // Use a variable name that definitely doesn't exist
        val result = envService.getVariable("INTELLIJ_DIRENV_TEST_NONEXISTENT_VAR_12345")
        assertNull(result)
    }

    @Test
    fun `getVariable returns value for existing system variable`() {
        // PATH should exist on any system
        val result = envService.getVariable("PATH")
        assertNotNull(result)
    }

    @Test
    fun `setVariable and getVariable work together`() {
        val testVar = "INTELLIJ_DIRENV_TEST_VAR_SET_GET"
        val testValue = "test_value_123"

        // Set the variable
        envService.setVariable(testVar, testValue)

        // Verify it can be retrieved
        assertEquals(testValue, envService.getVariable(testVar))

        // Cleanup
        envService.unsetVariable(testVar)
    }

    @Test
    fun `setVariable updates existing variable`() {
        val testVar = "INTELLIJ_DIRENV_TEST_VAR_UPDATE"
        val initialValue = "initial_value"
        val updatedValue = "updated_value"

        // Set initial value
        envService.setVariable(testVar, initialValue)
        assertEquals(initialValue, envService.getVariable(testVar))

        // Update value
        envService.setVariable(testVar, updatedValue)
        assertEquals(updatedValue, envService.getVariable(testVar))

        // Cleanup
        envService.unsetVariable(testVar)
    }

    @Test
    fun `unsetVariable removes variable`() {
        val testVar = "INTELLIJ_DIRENV_TEST_VAR_UNSET"
        val testValue = "value_to_remove"

        // Set the variable
        envService.setVariable(testVar, testValue)
        assertNotNull(envService.getVariable(testVar))

        // Unset the variable
        envService.unsetVariable(testVar)
        assertNull(envService.getVariable(testVar))
    }

    @Test
    fun `unsetVariable on non-existent variable does not throw`() {
        // Should not throw when unsetting a variable that doesn't exist
        assertDoesNotThrow {
            envService.unsetVariable("INTELLIJ_DIRENV_TEST_NONEXISTENT_VAR_TO_UNSET")
        }
    }

    @Test
    fun `setVariable handles empty value`() {
        val testVar = "INTELLIJ_DIRENV_TEST_VAR_EMPTY"

        envService.setVariable(testVar, "")
        assertEquals("", envService.getVariable(testVar))

        // Cleanup
        envService.unsetVariable(testVar)
    }

    @Test
    fun `setVariable handles special characters in value`() {
        val testVar = "INTELLIJ_DIRENV_TEST_VAR_SPECIAL"
        val specialValue = "path/to/file:with=special&chars"

        envService.setVariable(testVar, specialValue)
        assertEquals(specialValue, envService.getVariable(testVar))

        // Cleanup
        envService.unsetVariable(testVar)
    }
}
