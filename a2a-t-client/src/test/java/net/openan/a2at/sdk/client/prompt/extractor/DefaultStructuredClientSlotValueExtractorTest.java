package net.openan.a2at.sdk.client.prompt.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.openan.a2at.sdk.llm.LLMClient;
import net.openan.a2at.sdk.llm.adapter.LLMAdapter;
import net.openan.a2at.sdk.llm.model.LLMResponse;
import net.openan.a2at.sdk.llm.model.LlmUsage;
import net.openan.a2at.sdk.llm.model.StructuredGenerationRequest;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotDefinition;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotSchema;
import org.junit.jupiter.api.Test;

class DefaultStructuredClientSlotValueExtractorTest {

    @Test
    void extractSlotsBuildsStructuredPromptAndReturnsNormalizedSlots() throws IOException {
        RecordingAdapter adapter = new RecordingAdapter(
                "{\"slots\":{\"site\":\"Site A\",\"additional_notes\":null,\"limit\":\"5\",\"severity\":\"high\"},"
                        + "\"slot_errors\":[]}");
        LLMClient llmClient = buildClient(adapter);
        DefaultStructuredClientSlotValueExtractor extractor = new DefaultStructuredClientSlotValueExtractor(
                llmClient,
                (scenarioCode, language) -> new PromptSlotSchema(
                        scenarioCode,
                        List.of(
                                new PromptSlotDefinition("site", true, "string", "^Site .+", null, null, null, null),
                                new PromptSlotDefinition(
                                        "additional_notes", false, "string", null, null, null, null, null),
                                new PromptSlotDefinition("limit", false, "integer", null, 1.0d, 10.0d, null, null),
                                new PromptSlotDefinition(
                                        "severity",
                                        false,
                                        "string",
                                        null,
                                        null,
                                        null,
                                        List.of("low", "medium", "high"),
                                        null))),
                "Extract slots from the input.",
                "Return slots as JSON.");

        Map<String, String> slots = extractor.extractSlots(
                "Analyze Site A with critical severity.",
                "energy_saving",
                "en-US",
                "Site: {site}\nNotes: {additional_notes}\nLimit: {limit}\nSeverity: {severity}");

        assertEquals(
                Map.of(
                        "site", "Site A",
                        "additional_notes", "",
                        "limit", "5",
                        "severity", "high"),
                slots);
        assertEquals(2, adapter.lastRequest().messages().size());
        assertEquals("system", adapter.lastRequest().messages().get(0).role());
        assertTrue(adapter.lastRequest().messages().get(1).content().contains("energy_saving"));
        assertTrue(
                adapter.lastRequest().messages().get(1).content().contains("Analyze Site A with critical severity."));
        assertTrue(adapter.lastRequest().jsonSchema().containsKey("required"));
    }

    private static LLMClient buildClient(RecordingAdapter adapter) throws IOException {
        Path envFile = Files.createTempFile("a2at-slot-extractor", ".env");
        Files.writeString(
                envFile,
                """
                A2AT_LLM_PROVIDER=openai_compatible
                A2AT_LLM_MODEL=test-model
                A2AT_LLM_API_KEY=test-key
                """);
        return new LLMClient(envFile, adapter);
    }

    private static final class RecordingAdapter implements LLMAdapter {

        private final String payload;

        private StructuredGenerationRequest lastRequest;

        private RecordingAdapter(String payload) {
            this.payload = payload;
        }

        @Override
        public LLMResponse structured(StructuredGenerationRequest request) {
            this.lastRequest = request;
            return new LLMResponse(payload, "test-model", new LlmUsage(1, 1, 2), Map.of());
        }

        private StructuredGenerationRequest lastRequest() {
            return lastRequest;
        }
    }
}
