package net.openan.a2at.sdk.client.prompt.extractor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.openan.a2at.sdk.client.prompt.loader.ClientSlotSchemaLoader;
import net.openan.a2at.sdk.client.prompt.loader.DefaultClasspathClientSlotSchemaLoader;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotDefinition;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotSchema;
import net.openan.a2at.sdk.resources.ClasspathPromptResourceLoader;

/**
 * Minimal slot extractor that derives output keys from slot schema and applies lightweight constraints.
 *
 * @since 2026-06
 */
public final class DefaultTemplateDrivenSlotValueExtractor implements ClientSlotValueExtractor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{?\\s*([^{}]+?)\\s*\\}\\}?");

    private final ClientSlotSchemaLoader slotSchemaLoader;

    /**
     * Creates a default schema-driven extractor backed by packaged slot resources.
     */
    public DefaultTemplateDrivenSlotValueExtractor() {
        this(new DefaultClasspathClientSlotSchemaLoader(new ClasspathPromptResourceLoader()));
    }

    /**
     * Creates a schema-driven extractor backed by the provided slot schema loader.
     *
     * @param slotSchemaLoader slot schema loader
     */
    public DefaultTemplateDrivenSlotValueExtractor(ClientSlotSchemaLoader slotSchemaLoader) {
        this.slotSchemaLoader = slotSchemaLoader;
    }

    @Override
    public Map<String, String> extractSlots(
            Object userInput, String scenarioCode, String language, String templateText) {
        Map<String, String> slots = new LinkedHashMap<>();
        Map<String, Boolean> templateSlotNames = referencedTemplateSlots(templateText);
        PromptSlotSchema schema = slotSchemaLoader.loadSlotSchema(scenarioCode, language);
        for (PromptSlotDefinition definition : schema.slotDefinitions()) {
            if (!definition.required() && !templateSlotNames.containsKey(definition.name())) {
                continue;
            }
            String value = normalizeAndValidate(resolveSlotValue(userInput, definition.name()), definition);
            slots.put(definition.name(), value);
        }
        return slots;
    }

    private static Map<String, Boolean> referencedTemplateSlots(String templateText) {
        Map<String, Boolean> slotNames = new LinkedHashMap<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateText);
        while (matcher.find()) {
            slotNames.putIfAbsent(matcher.group(1).trim(), Boolean.TRUE);
        }
        return slotNames;
    }

    private static String resolveSlotValue(Object userInput, String slotName) {
        if (userInput instanceof Map<?, ?> mapValue) {
            Object rawValue = mapValue.get(slotName);
            return rawValue == null ? "" : String.valueOf(rawValue);
        }
        if ("input".equals(slotName)) {
            return String.valueOf(userInput);
        }
        return "";
    }

    private static String normalizeAndValidate(String value, PromptSlotDefinition definition) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (definition.allowedValues() != null
                && !definition.allowedValues().isEmpty()
                && !definition.allowedValues().contains(normalized)) {
            return "";
        }
        if (definition.pattern() != null
                && !definition.pattern().isBlank()
                && !normalized.matches(definition.pattern())) {
            return "";
        }
        if ("integer".equalsIgnoreCase(definition.jsonType()) || "number".equalsIgnoreCase(definition.jsonType())) {
            try {
                double numericValue = Double.parseDouble(normalized);
                if (definition.minimum() != null && numericValue < definition.minimum()) {
                    return "";
                }
                if (definition.maximum() != null && numericValue > definition.maximum()) {
                    return "";
                }
                if ("integer".equalsIgnoreCase(definition.jsonType())) {
                    return String.valueOf((int) numericValue);
                }
                return stripTrailingZero(numericValue);
            } catch (NumberFormatException error) {
                return "";
            }
        }
        return normalized;
    }

    private static String stripTrailingZero(double value) {
        if (Math.rint(value) == value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
