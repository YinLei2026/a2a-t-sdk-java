package net.openan.a2at.sdk.server.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.openan.a2at.sdk.core.exception.ResourceNotFoundException;
import net.openan.a2at.sdk.llm.LLMClient;
import net.openan.a2at.sdk.llm.adapter.LLMAdapter;
import net.openan.a2at.sdk.llm.model.LLMResponse;
import net.openan.a2at.sdk.llm.model.LlmUsage;
import net.openan.a2at.sdk.llm.model.StructuredGenerationRequest;
import net.openan.a2at.sdk.prompt.analysis.impl.PromptSlotValueExtractor;
import net.openan.a2at.sdk.prompt.analysis.impl.ScenarioRecognizer;
import net.openan.a2at.sdk.prompt.analysis.model.StructuredSlotExtractionResult;
import net.openan.a2at.sdk.prompt.analysis.model.ScenarioRecognitionResult;
import net.openan.a2at.sdk.prompt.analysis.model.StructuredSlotValidationError;
import net.openan.a2at.sdk.prompt.resources.loader.PromptSlotSchemaLoader;
import net.openan.a2at.sdk.prompt.resources.loader.PromptTemplateTextLoader;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotDefinition;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotSchema;
import net.openan.a2at.sdk.prompt.resources.model.ScenarioDefinition;
import net.openan.a2at.sdk.server.exception.PromptComplianceCheckException;
import net.openan.a2at.sdk.server.model.ProcessedPromptMetadata;
import org.junit.jupiter.api.Test;

class LlmBackedPromptMetadataExtractorTest {

    @Test
    void extractResolvesScenarioLoadsTemplateAndReturnsExtractedSlots() throws IOException {
        LLMClient llmClient = buildClient("{\"matched\":true,\"scenario_code\":\"subscribe_incident\",\"error_message\":null}");
        PromptTemplateTextLoader templateLoader = (scenarioCode, language) -> "## 通知主题\n{{通知主题}}\n";
        PromptSlotSchemaLoader slotSchemaLoader = (scenarioCode, language) -> new PromptSlotSchema(
                scenarioCode, List.of(new PromptSlotDefinition("通知主题", true, "string", null, null, null, null, null)));
        PromptSlotValueExtractor slotValueExtractor = (userInput, scenarioCode, language) -> new StructuredSlotExtractionResult(
                Map.of("通知主题", "Incident"), List.of());
        LlmBackedPromptMetadataExtractor extractor = new LlmBackedPromptMetadataExtractor(
                new ScenarioRecognizer(llmClient),
                List.of(new ScenarioDefinition("subscribe_incident", "Incident 事件订阅", "订阅事件", "订阅Incident")),
                "zh-CN",
                "Identify scenario.",
                "Choose scenario.",
                templateLoader,
                slotSchemaLoader,
                slotValueExtractor);

        ProcessedPromptMetadata metadata = extractor.extract("## 通知主题\nIncident\n");

        assertEquals("subscribe_incident", metadata.scenarioCode());
        assertEquals("zh-CN", metadata.language());
        assertEquals("## 通知主题\n{{通知主题}}\n", metadata.templateText());
        assertEquals(Map.of("通知主题", "Incident"), metadata.slots());
    }

    @Test
    void extractReturnsPromptParseErrorWhenScenarioRecognitionDoesNotMatch() throws IOException {
        LLMClient llmClient = buildClient("{\"matched\":false,\"scenario_code\":null,\"error_message\":\"No scenario matched.\"}");
        LlmBackedPromptMetadataExtractor extractor = new LlmBackedPromptMetadataExtractor(
                new ScenarioRecognizer(llmClient),
                List.of(new ScenarioDefinition("subscribe_incident", "Incident 事件订阅", "订阅事件", "订阅Incident")),
                "zh-CN",
                "Identify scenario.",
                "Choose scenario.",
                (scenarioCode, language) -> "",
                (scenarioCode, language) -> new PromptSlotSchema(scenarioCode, List.of()),
                (userInput, scenarioCode, language) -> new StructuredSlotExtractionResult(Map.of(), List.of()));

        PromptComplianceCheckException error =
                assertThrows(PromptComplianceCheckException.class, () -> extractor.extract("unknown prompt"));

        assertEquals("processed_prompt_parse_error", error.code());
        assertEquals("prompt_parse", error.stage());
    }

    @Test
    void extractReturnsSlotValidationErrorWhenStructuredExtractionReportsSlotErrors() throws IOException {
        LLMClient llmClient = buildClient("{\"matched\":true,\"scenario_code\":\"subscribe_incident\",\"error_message\":null}");
        LlmBackedPromptMetadataExtractor extractor = new LlmBackedPromptMetadataExtractor(
                new ScenarioRecognizer(llmClient),
                List.of(new ScenarioDefinition("subscribe_incident", "Incident 事件订阅", "订阅事件", "订阅Incident")),
                "zh-CN",
                "Identify scenario.",
                "Choose scenario.",
                (scenarioCode, language) -> "template",
                (scenarioCode, language) -> new PromptSlotSchema(
                        scenarioCode,
                        List.of(new PromptSlotDefinition("通知主题", true, "string", null, null, null, null, null))),
                (userInput, scenarioCode, language) -> new StructuredSlotExtractionResult(
                        Map.of("通知主题", ""),
                        List.of(new StructuredSlotValidationError("通知主题", "missing_input", "通知主题缺失"))));

        PromptComplianceCheckException error =
                assertThrows(PromptComplianceCheckException.class, () -> extractor.extract("bad prompt"));

        assertEquals("slot_validation_error", error.code());
        assertEquals("slot_validation", error.stage());
    }

    @Test
    void extractPropagatesTemplateLoadErrorsAsGenerationFailures() throws IOException {
        LLMClient llmClient = buildClient("{\"matched\":true,\"scenario_code\":\"subscribe_incident\",\"error_message\":null}");
        LlmBackedPromptMetadataExtractor extractor = new LlmBackedPromptMetadataExtractor(
                new ScenarioRecognizer(llmClient),
                List.of(new ScenarioDefinition("subscribe_incident", "Incident 事件订阅", "订阅事件", "订阅Incident")),
                "zh-CN",
                "Identify scenario.",
                "Choose scenario.",
                (scenarioCode, language) -> {
                    throw new ResourceNotFoundException("Prompt resource file does not exist.", "template.md");
                },
                (scenarioCode, language) -> new PromptSlotSchema(scenarioCode, List.of()),
                (userInput, scenarioCode, language) -> new StructuredSlotExtractionResult(Map.of(), List.of()));

        PromptComplianceCheckException error =
                assertThrows(PromptComplianceCheckException.class, () -> extractor.extract("prompt"));

        assertEquals("template_not_found", error.code());
        assertEquals("generation", error.stage());
    }

    private static LLMClient buildClient(String payload) throws IOException {
        Path envFile = Files.createTempFile("a2at-server-metadata-extractor", ".env");
        Files.writeString(
                envFile,
                """
                A2AT_LLM_PROVIDER=openai_compatible
                A2AT_LLM_MODEL=test-model
                A2AT_LLM_API_KEY=test-key
                """);
        return new LLMClient(envFile, new RecordingAdapter(payload));
    }

    private static final class RecordingAdapter implements LLMAdapter {
        private final String payload;

        private RecordingAdapter(String payload) {
            this.payload = payload;
        }

        @Override
        public LLMResponse structured(StructuredGenerationRequest request) {
            return new LLMResponse(payload, "test-model", new LlmUsage(1, 1, 2), Map.of());
        }
    }
}
