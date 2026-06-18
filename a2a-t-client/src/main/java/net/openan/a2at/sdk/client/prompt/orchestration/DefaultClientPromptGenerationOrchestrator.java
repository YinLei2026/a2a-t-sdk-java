package net.openan.a2at.sdk.client.prompt.orchestration;

import java.util.List;
import java.util.Map;
import net.openan.a2at.sdk.client.model.PromptGenerationFailure;
import net.openan.a2at.sdk.client.model.PromptGenerationResult;
import net.openan.a2at.sdk.client.prompt.extractor.ClientSlotValueExtractor;
import net.openan.a2at.sdk.client.prompt.loader.ClientTemplateLoader;
import net.openan.a2at.sdk.client.prompt.recognition.ClientScenarioRecognizer;
import net.openan.a2at.sdk.core.exception.ResourceNotFoundException;
import net.openan.a2at.sdk.prompt.analysis.model.ScenarioRecognitionResult;
import net.openan.a2at.sdk.prompt.resources.model.ScenarioDefinition;
import net.openan.a2at.sdk.prompt.taskrendering.api.TaskPromptRenderer;
import net.openan.a2at.sdk.prompt.taskrendering.exception.TaskPromptRenderException;

/**
 * Minimal runnable client prompt generation orchestrator.
 *
 * @since 2026-06
 */
public final class DefaultClientPromptGenerationOrchestrator implements ClientPromptGenerationOrchestrator {

    private final ClientScenarioRecognizer scenarioRecognizer;

    private final List<ScenarioDefinition> scenarios;

    private final String language;

    private final String systemPrompt;

    private final String userPrompt;

    private final ClientTemplateLoader templateLoader;

    private final ClientSlotValueExtractor slotValueExtractor;

    private final TaskPromptRenderer renderer;

    private String lastNormalizedInput;

    /**
     * Creates a client prompt-generation orchestrator with explicit collaborators.
     *
     * @param scenarioRecognizer scenario recognizer
     * @param scenarios supported scenario definitions
     * @param language locale identifier for resource lookup
     * @param systemPrompt system prompt for scenario recognition
     * @param userPrompt user prompt for scenario recognition
     * @param templateLoader template loader
     * @param slotValueExtractor slot value extractor
     * @param renderer task prompt renderer
     */
    public DefaultClientPromptGenerationOrchestrator(
            ClientScenarioRecognizer scenarioRecognizer,
            List<ScenarioDefinition> scenarios,
            String language,
            String systemPrompt,
            String userPrompt,
            ClientTemplateLoader templateLoader,
            ClientSlotValueExtractor slotValueExtractor,
            TaskPromptRenderer renderer) {
        this.scenarioRecognizer = scenarioRecognizer;
        this.scenarios = scenarios;
        this.language = language;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.templateLoader = templateLoader;
        this.slotValueExtractor = slotValueExtractor;
        this.renderer = renderer;
    }

    @Override
    public PromptGenerationResult generateTaskPrompt(Object userInput) {
        String normalizedInput = String.valueOf(userInput);
        this.lastNormalizedInput = normalizedInput;

        final ScenarioRecognitionResult recognition;
        try {
            recognition = scenarioRecognizer.recognize(normalizedInput, scenarios, systemPrompt, userPrompt);
        } catch (ResourceNotFoundException error) {
            return PromptGenerationResult.failure(
                    new PromptGenerationFailure("prompt_resource_load_error", error.getMessage(), "generation"));
        }
        if (!recognition.matched()
                || recognition.scenarioCode() == null
                || recognition.scenarioCode().isBlank()) {
            return PromptGenerationResult.failure(new PromptGenerationFailure(
                    "scenario_not_matched",
                    recognition.errorMessage() == null ? "Scenario recognition failed." : recognition.errorMessage(),
                    "scenario"));
        }

        final String templateText;
        try {
            templateText = templateLoader.loadTemplate(recognition.scenarioCode(), language);
        } catch (ResourceNotFoundException error) {
            return PromptGenerationResult.failure(
                    new PromptGenerationFailure("template_not_found", error.getMessage(), "generation"));
        }

        try {
            Map<String, String> slots =
                    slotValueExtractor.extractSlots(userInput, recognition.scenarioCode(), language, templateText);
            String renderedPrompt = renderer.render(templateText, slots);
            return PromptGenerationResult.success(renderedPrompt);
        } catch (TaskPromptRenderException error) {
            return PromptGenerationResult.failure(
                    new PromptGenerationFailure("render_failed", error.getMessage(), "generation"));
        }
    }

    String lastNormalizedInput() {
        return lastNormalizedInput;
    }
}
