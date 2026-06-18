package net.openan.a2at.sdk.client.prompt.extractor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotDefinition;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotSchema;
import org.junit.jupiter.api.Test;

class DefaultTemplateDrivenSlotValueExtractorTest {

    @Test
    void extractSlotsUsesOnlyTemplateReferencedKeysFromMapInput() {
        DefaultTemplateDrivenSlotValueExtractor extractor =
                new DefaultTemplateDrivenSlotValueExtractor((scenarioCode, language) -> schema(
                        scenarioCode,
                        new PromptSlotDefinition("site", true, "string", null, null, null, null, null),
                        new PromptSlotDefinition("additional_notes", false, "string", null, null, null, null, null),
                        new PromptSlotDefinition("ignored", false, "string", null, null, null, null, null)));

        Map<String, String> slots = extractor.extractSlots(
                Map.of("site", "Site A", "additional_notes", "critical", "ignored", "value"),
                "energy_saving",
                "en-US",
                "Site: {site}\nNotes: {additional_notes}");

        assertEquals(Map.of("site", "Site A", "additional_notes", "critical"), slots);
    }

    @Test
    void extractSlotsMapsStringInputToSchemaDefinedInputSlot() {
        DefaultTemplateDrivenSlotValueExtractor extractor =
                new DefaultTemplateDrivenSlotValueExtractor((scenarioCode, language) -> schema(
                        scenarioCode, new PromptSlotDefinition("input", true, "string", null, null, null, null, null)));

        Map<String, String> slots = extractor.extractSlots("Analyze Site A.", "free_text", "en-US", "Input: {input}");

        assertEquals(Map.of("input", "Analyze Site A."), slots);
    }

    @Test
    void extractSlotsUsesNonAsciiTemplateReferencedKeysFromMapInput() {
        DefaultTemplateDrivenSlotValueExtractor extractor =
                new DefaultTemplateDrivenSlotValueExtractor((scenarioCode, language) -> schema(
                        scenarioCode,
                        new PromptSlotDefinition("通知主题", false, "string", null, null, null, null, null),
                        new PromptSlotDefinition("订阅条件", false, "string", null, null, null, null, null),
                        new PromptSlotDefinition("上报通知数据格式", false, "string", null, null, null, null, null)));

        Map<String, String> slots = extractor.extractSlots(
                Map.of(
                        "通知主题", "Incident",
                        "订阅条件", "故障优先级为：严重",
                        "上报通知数据格式", "通过DataPart上报Incident数据"),
                "subscribe_incident",
                "zh-CN",
                "通知主题: {{通知主题}}\n订阅条件: {{订阅条件}}\n上报格式: {{上报通知数据格式}}");

        assertEquals(
                Map.of(
                        "通知主题", "Incident",
                        "订阅条件", "故障优先级为：严重",
                        "上报通知数据格式", "通过DataPart上报Incident数据"),
                slots);
    }

    @Test
    void extractSlotsBlanksValueWhenSchemaConstraintIsNotSatisfied() {
        DefaultTemplateDrivenSlotValueExtractor extractor =
                new DefaultTemplateDrivenSlotValueExtractor((scenarioCode, language) -> schema(
                        scenarioCode,
                        new PromptSlotDefinition("site", true, "string", "^Site .+", null, null, null, null),
                        new PromptSlotDefinition("additional_notes", false, "string", null, null, null, null, null)));

        Map<String, String> slots = extractor.extractSlots(
                Map.of("site", "invalid", "additional_notes", "critical"),
                "energy_saving",
                "en-US",
                "Site: {site}\nNotes: {additional_notes}");

        assertEquals(Map.of("site", "", "additional_notes", "critical"), slots);
    }

    @Test
    void extractSlotsNormalizesAndFiltersEnumAndNumericConstraints() {
        DefaultTemplateDrivenSlotValueExtractor extractor =
                new DefaultTemplateDrivenSlotValueExtractor((scenarioCode, language) -> schema(
                        scenarioCode,
                        new PromptSlotDefinition("site", true, "string", "^Site .+", null, null, null, null),
                        new PromptSlotDefinition("additional_notes", false, "string", null, null, null, null, null),
                        new PromptSlotDefinition("limit", false, "integer", null, 1.0d, 10.0d, null, null),
                        new PromptSlotDefinition(
                                "severity",
                                false,
                                "string",
                                null,
                                null,
                                null,
                                List.of("low", "medium", "high"),
                                null)));

        Map<String, String> slots = extractor.extractSlots(
                Map.of(
                        "site", "Site A",
                        "additional_notes", "critical",
                        "limit", "5",
                        "severity", "high"),
                "energy_saving",
                "en-US",
                "Site: {site}\nNotes: {additional_notes}\nLimit: {limit}\nSeverity: {severity}");

        assertEquals(
                Map.of(
                        "site", "Site A",
                        "additional_notes", "critical",
                        "limit", "5",
                        "severity", "high"),
                slots);
    }

    @Test
    void extractSlotsBlanksValuesWhenEnumOrRangeConstraintsFail() {
        DefaultTemplateDrivenSlotValueExtractor extractor =
                new DefaultTemplateDrivenSlotValueExtractor((scenarioCode, language) -> schema(
                        scenarioCode,
                        new PromptSlotDefinition("site", true, "string", "^Site .+", null, null, null, null),
                        new PromptSlotDefinition("additional_notes", false, "string", null, null, null, null, null),
                        new PromptSlotDefinition("limit", false, "integer", null, 1.0d, 10.0d, null, null),
                        new PromptSlotDefinition(
                                "severity",
                                false,
                                "string",
                                null,
                                null,
                                null,
                                List.of("low", "medium", "high"),
                                null)));

        Map<String, String> slots = extractor.extractSlots(
                Map.of(
                        "site", "Site A",
                        "additional_notes", "",
                        "limit", "50",
                        "severity", "urgent"),
                "energy_saving",
                "en-US",
                "Site: {site}\nNotes: {additional_notes}\nLimit: {limit}\nSeverity: {severity}");

        assertEquals(
                Map.of(
                        "site", "Site A",
                        "additional_notes", "",
                        "limit", "",
                        "severity", ""),
                slots);
    }

    private static PromptSlotSchema schema(String scenarioCode, PromptSlotDefinition... definitions) {
        return new PromptSlotSchema(scenarioCode, List.of(definitions));
    }
}
