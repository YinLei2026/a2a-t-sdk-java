package net.openan.a2at.sdk.server.metadata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.openan.a2at.sdk.prompt.taskrendering.api.TaskPromptRenderer;
import net.openan.a2at.sdk.prompt.taskrendering.exception.TaskPromptRenderException;
import net.openan.a2at.sdk.server.exception.PromptComplianceCheckException;
import net.openan.a2at.sdk.server.model.ProcessedPromptMetadata;
import net.openan.a2at.sdk.server.model.PromptTemplateDefinition;
import net.openan.a2at.sdk.server.model.PromptTemplateSlotDefinition;

/**
 * Extracts prompt metadata by matching processed prompts against known templates.
 *
 * @since 2026-06
 */
public final class TemplateMatchingPromptMetadataExtractor implements ServerPromptMetadataExtractor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{?\\s*([^{}]+?)\\s*\\}\\}?");
    private static final String SLOT_SENTINEL_PREFIX = "__A2AT_SLOT_";
    private static final String SLOT_SENTINEL_SUFFIX = "__";

    private final List<PromptTemplateDefinition> templates;
    private final TaskPromptRenderer renderer;

    /**
     * Creates a template-matching metadata extractor.
     *
     * @param templates known prompt templates
     */
    public TemplateMatchingPromptMetadataExtractor(List<PromptTemplateDefinition> templates) {
        this(templates, new TaskPromptRenderer());
    }

    /**
     * Creates a template-matching metadata extractor backed by the supplied renderer.
     *
     * @param templates known prompt templates
     * @param renderer task prompt renderer used to normalize template structure
     */
    public TemplateMatchingPromptMetadataExtractor(List<PromptTemplateDefinition> templates, TaskPromptRenderer renderer) {
        this.templates = List.copyOf(templates);
        this.renderer = renderer;
    }

    @Override
    public ProcessedPromptMetadata extract(String processedPromptText) {
        for (PromptTemplateDefinition template : templates) {
            Map<String, String> slots = tryExtractSlots(processedPromptText, template);
            if (slots != null) {
                validateRequiredSlots(slots, template.slotDefinitions());
                return new ProcessedPromptMetadata(
                        template.scenarioCode(), template.language(), template.templateText(), Map.copyOf(slots));
            }
        }
        throw new PromptComplianceCheckException(
                "processed_prompt_parse_error", "Prompt does not match any known template.", "prompt_parse");
    }

    private Map<String, String> tryExtractSlots(String processedPromptText, PromptTemplateDefinition template) {
        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(template.templateText());
        List<String> slotNames = new java.util.ArrayList<>();
        Map<String, String> sentinelSlots = new LinkedHashMap<>();
        int slotIndex = 0;
        while (placeholderMatcher.find()) {
            String slotName = placeholderMatcher.group(1).trim();
            String sentinel = SLOT_SENTINEL_PREFIX + slotIndex + SLOT_SENTINEL_SUFFIX;
            slotNames.add(slotName);
            sentinelSlots.put(slotName, sentinel);
            slotIndex++;
        }

        final String normalizedTemplateText;
        try {
            normalizedTemplateText = renderer.render(template.templateText(), sentinelSlots);
        } catch (TaskPromptRenderException error) {
            throw new PromptComplianceCheckException("processed_prompt_parse_error", error.getMessage(), "prompt_parse");
        }

        StringBuilder pattern = new StringBuilder("^");
        int cursor = 0;
        for (int index = 0; index < slotNames.size(); index++) {
            String sentinel = sentinelSlots.get(slotNames.get(index));
            int sentinelIndex = normalizedTemplateText.indexOf(sentinel, cursor);
            if (sentinelIndex < 0) {
                return null;
            }
            pattern.append(Pattern.quote(normalizedTemplateText.substring(cursor, sentinelIndex)));
            pattern.append("(.*?)");
            cursor = sentinelIndex + sentinel.length();
        }
        pattern.append(Pattern.quote(normalizedTemplateText.substring(cursor)));
        pattern.append("$");

        Matcher match = Pattern.compile(pattern.toString(), Pattern.DOTALL).matcher(processedPromptText);
        if (!match.matches()) {
            return null;
        }

        Map<String, String> slots = new LinkedHashMap<>();
        for (int index = 0; index < slotNames.size(); index++) {
            slots.put(slotNames.get(index), match.group(index + 1));
        }
        return slots;
    }

    private static void validateRequiredSlots(
            Map<String, String> slots, List<PromptTemplateSlotDefinition> slotDefinitions) {
        for (PromptTemplateSlotDefinition definition : slotDefinitions) {
            String value = slots.get(definition.name());
            if (definition.required() && (value == null || value.isBlank())) {
                throw new PromptComplianceCheckException(
                        "slot_validation_error",
                        "Required slot '" + definition.name() + "' is missing.",
                        "slot_validation");
            }
        }
    }
}
