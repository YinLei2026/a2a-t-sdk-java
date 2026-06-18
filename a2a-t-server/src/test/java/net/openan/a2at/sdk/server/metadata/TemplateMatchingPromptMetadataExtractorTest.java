package net.openan.a2at.sdk.server.metadata;

import net.openan.a2at.sdk.server.exception.PromptComplianceCheckException;
import net.openan.a2at.sdk.server.model.ProcessedPromptMetadata;
import net.openan.a2at.sdk.server.model.PromptTemplateDefinition;
import net.openan.a2at.sdk.server.model.PromptTemplateSlotDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.openan.a2at.sdk.prompt.taskrendering.api.TaskPromptRenderer;
import org.junit.jupiter.api.Test;

class TemplateMatchingPromptMetadataExtractorTest {

    @Test
    void extractReturnsScenarioLanguageTemplateAndSlotsForMatchedTemplate() {
        TemplateMatchingPromptMetadataExtractor extractor =
                new TemplateMatchingPromptMetadataExtractor(List.of(new PromptTemplateDefinition(
                        "energy_saving",
                        "en-US",
                        "Site: {site}\nNotes: {additional_notes}",
                        List.of(
                                new PromptTemplateSlotDefinition("site", true),
                                new PromptTemplateSlotDefinition("additional_notes", false)))));

        ProcessedPromptMetadata metadata = extractor.extract("Site: Site A\nNotes: critical");

        assertEquals("energy_saving", metadata.scenarioCode());
        assertEquals("en-US", metadata.language());
        assertEquals("Site: {site}\nNotes: {additional_notes}", metadata.templateText());
        assertEquals(Map.of("site", "Site A", "additional_notes", "critical"), metadata.slots());
    }

    @Test
    void extractRejectsPromptThatDoesNotMatchAnyKnownTemplate() {
        TemplateMatchingPromptMetadataExtractor extractor =
                new TemplateMatchingPromptMetadataExtractor(List.of(new PromptTemplateDefinition(
                        "energy_saving",
                        "en-US",
                        "Site: {site}",
                        List.of(new PromptTemplateSlotDefinition("site", true)))));

        PromptComplianceCheckException error =
                assertThrows(PromptComplianceCheckException.class, () -> extractor.extract("Unknown prompt"));

        assertEquals("processed_prompt_parse_error", error.code());
        assertEquals("prompt_parse", error.stage());
    }

    @Test
    void extractRejectsMissingRequiredSlotValueAfterTemplateMatch() {
        TemplateMatchingPromptMetadataExtractor extractor =
                new TemplateMatchingPromptMetadataExtractor(List.of(new PromptTemplateDefinition(
                        "energy_saving",
                        "en-US",
                        "Site: {site}",
                        List.of(new PromptTemplateSlotDefinition("site", true)))));

        PromptComplianceCheckException error =
                assertThrows(PromptComplianceCheckException.class, () -> extractor.extract("Site: "));

        assertEquals("slot_validation_error", error.code());
        assertEquals("slot_validation", error.stage());
    }

    @Test
    void extractMatchesTemplateWithNonAsciiPlaceholders() {
        TemplateMatchingPromptMetadataExtractor extractor =
                new TemplateMatchingPromptMetadataExtractor(List.of(new PromptTemplateDefinition(
                        "subscribe_incident",
                        "zh-CN",
                        "通知主题: {{通知主题}}\n订阅条件: {{订阅条件}}",
                        List.of(
                                new PromptTemplateSlotDefinition("通知主题", true),
                                new PromptTemplateSlotDefinition("订阅条件", false)))));

        ProcessedPromptMetadata metadata = extractor.extract(
                "通知主题: Incident\n订阅条件: 故障优先级为：严重；告警类型为：flash");

        assertEquals("subscribe_incident", metadata.scenarioCode());
        assertEquals("zh-CN", metadata.language());
        assertEquals(
                Map.of(
                        "通知主题", "Incident",
                        "订阅条件", "故障优先级为：严重；告警类型为：flash"),
                metadata.slots());
    }

    @Test
    void extractMatchesCollapsedPromptAgainstOriginalTemplateWithDescriptiveLines() {
        String originalTemplate =
                "## 订阅描述\n"
                        + "请根据以下 <通知主题>、<订阅条件>、<上报通知数据格式>及<预期输出> 信息，完成网络侧智能故障Incident订阅与上报任务。\n\n"
                        + "## 通知主题\n"
                        + "{{通知主题}}（必选）\n"
                        + "要求：提供智能故障Incident的主题名称，具体名称可以是Incident、Fault、智能故障、故障等。\n\n"
                        + "## 订阅条件\n"
                        + "{{订阅条件}}（可选）\n"
                        + "要求：订阅条件包括故障优先级，故障名称。\n"
                        + "故障优先级：支持传入列表，该参数的取值范围包括 严重、高、中和低。\n"
                        + "故障名称：支持传入列表，该参数的取值范围为 网络侧故障的名称列表。例如：尾纤故障，光纤中断，单板故障，光模块故障等。\n\n"
                        + "## 上报通知数据格式\n"
                        + "{{上报通知数据格式}}（可选）\n"
                        + "要求：1、上报的数据类型：Incident、故障；2、上报的数据格式：基于A2A的哪一种Part承载（DataPart、TextPart）\n"
                        + "例如：通过DataPart上报Incident数据\n\n"
                        + "## 预期输出\n"
                        + "1、订阅结果，成功或失败\n"
                        + "2、订阅失败原因（可选）";
        String processedPrompt =
                "## 订阅描述\n"
                        + "请根据以下 <通知主题>、<订阅条件>、<上报通知数据格式>及<预期输出> 信息，完成网络侧智能故障Incident订阅与上报任务。\n\n"
                        + "## 通知主题\n"
                        + "Incident\n\n"
                        + "## 订阅条件\n"
                        + "订阅级别为critical的ETH-LOS的故障\n\n"
                        + "## 上报通知数据格式\n"
                        + "DataPart\n\n"
                        + "## 预期输出\n"
                        + "1、订阅结果，成功或失败\n"
                        + "2、订阅失败原因（可选）";
        Map<String, String> sentinelSlots = new LinkedHashMap<>();
        sentinelSlots.put("通知主题", "__A2AT_SLOT_0__");
        sentinelSlots.put("订阅条件", "__A2AT_SLOT_1__");
        sentinelSlots.put("上报通知数据格式", "__A2AT_SLOT_2__");

        assertEquals(
                "## 订阅描述\n"
                        + "请根据以下 <通知主题>、<订阅条件>、<上报通知数据格式>及<预期输出> 信息，完成网络侧智能故障Incident订阅与上报任务。\n\n"
                        + "## 通知主题\n"
                        + "__A2AT_SLOT_0__\n\n"
                        + "## 订阅条件\n"
                        + "__A2AT_SLOT_1__\n\n"
                        + "## 上报通知数据格式\n"
                        + "__A2AT_SLOT_2__\n\n"
                        + "## 预期输出\n"
                        + "1、订阅结果，成功或失败\n"
                        + "2、订阅失败原因（可选）",
                new TaskPromptRenderer().render(originalTemplate, sentinelSlots));

        TemplateMatchingPromptMetadataExtractor extractor =
                new TemplateMatchingPromptMetadataExtractor(List.of(new PromptTemplateDefinition(
                        "subscribe_incident",
                        "zh-CN",
                        originalTemplate,
                        List.of(
                                new PromptTemplateSlotDefinition("通知主题", true),
                                new PromptTemplateSlotDefinition("订阅条件", false),
                                new PromptTemplateSlotDefinition("上报通知数据格式", false)))));

        ProcessedPromptMetadata metadata = extractor.extract(processedPrompt);

        assertEquals("subscribe_incident", metadata.scenarioCode());
        assertEquals("zh-CN", metadata.language());
        assertEquals(
                Map.of(
                        "通知主题", "Incident",
                        "订阅条件", "订阅级别为critical的ETH-LOS的故障",
                        "上报通知数据格式", "DataPart"),
                metadata.slots());
    }
}
