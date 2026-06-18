package net.openan.a2at.sdk.client.prompt.orchestration;

import net.openan.a2at.sdk.client.model.PromptGenerationResult;

/**
 * Internal prompt-generation orchestration contract used by the client facade.
 *
 * @since 2026-06
 */
public interface ClientPromptGenerationOrchestrator {

    /**
     * Generates a processed task prompt from user input.
     *
     * @param userInput raw or structured user input
     * @return prompt-generation result
     */
    PromptGenerationResult generateTaskPrompt(Object userInput);
}
