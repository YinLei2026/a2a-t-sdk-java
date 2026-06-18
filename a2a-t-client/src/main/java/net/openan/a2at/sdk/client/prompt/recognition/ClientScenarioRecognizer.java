package net.openan.a2at.sdk.client.prompt.recognition;

import java.util.List;
import net.openan.a2at.sdk.prompt.analysis.model.ScenarioRecognitionResult;
import net.openan.a2at.sdk.prompt.resources.model.ScenarioDefinition;

/**
 * Client-side adapter for scenario recognition.
 *
 * @since 2026-06
 */
@FunctionalInterface
public interface ClientScenarioRecognizer {

    /**
     * Resolves one normalized user input into a scenario recognition result.
     *
     * @param normalizedInput normalized user input text
     * @param scenarios available scenarios
     * @param systemPrompt scenario-recognition system prompt
     * @param userPrompt scenario-recognition user prompt
     * @return recognition result describing whether a scenario matched
     */
    ScenarioRecognitionResult recognize(
            String normalizedInput, List<ScenarioDefinition> scenarios, String systemPrompt, String userPrompt);
}
