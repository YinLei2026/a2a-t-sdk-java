package net.openan.a2at.sdk.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PromptGenerationConfig}.
 *
 * <p>Tests cover the construction of prompt generation configuration
 * with all required fields.
 *
 * @since 2026-06
 */
class PromptGenerationConfigTest {

    /**
     * Verifies that {@link PromptGenerationConfig} can be constructed with all
     * prompt-related fields.
     *
     * <p>Scenario: Create a config with language, scenario prompts (system and user),
     * and slot prompts (system and user).
     * Expected result: All fields are correctly accessible via their accessor methods.
     */
    @Test
    void should_createConfigWithAllFields_When_constructorCalled() {
        PromptGenerationConfig config = new PromptGenerationConfig(
                "zh-CN",
                "scenario-system",
                "scenario-user",
                "slot-system",
                "slot-user");

        assertEquals("zh-CN", config.language());
        assertEquals("scenario-system", config.scenarioSystemPrompt());
        assertEquals("scenario-user", config.scenarioUserPrompt());
        assertEquals("slot-system", config.slotSystemPrompt());
        assertEquals("slot-user", config.slotUserPrompt());
    }
}