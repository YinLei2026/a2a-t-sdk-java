package net.openan.a2at.sdk.server.assembly;

import java.util.List;
import net.openan.a2at.sdk.prompt.resources.loader.ClasspathPromptSlotSchemaLoader;
import net.openan.a2at.sdk.prompt.resources.loader.ClasspathPromptTemplateLoader;
import net.openan.a2at.sdk.prompt.resources.loader.PromptSlotSchemaLoader;
import net.openan.a2at.sdk.prompt.resources.loader.PromptTemplateTextLoader;
import net.openan.a2at.sdk.prompt.resources.model.PromptSlotSchema;
import net.openan.a2at.sdk.prompt.resources.model.ScenarioDefinition;
import net.openan.a2at.sdk.resources.ClasspathPromptResourceLoader;
import net.openan.a2at.sdk.server.model.PromptTemplateDefinition;
import net.openan.a2at.sdk.server.model.PromptTemplateSlotDefinition;

/**
 * Loads server-side prompt template definitions from packaged classpath prompt resources.
 *
 * @since 2026-06
 */
public final class ClasspathServerPromptTemplateLoader {

    private final PromptTemplateTextLoader templateLoader;

    private final PromptSlotSchemaLoader slotSchemaLoader;

    public ClasspathServerPromptTemplateLoader(ClasspathPromptResourceLoader resourceLoader) {
        this.templateLoader = new ClasspathPromptTemplateLoader(resourceLoader);
        this.slotSchemaLoader = new ClasspathPromptSlotSchemaLoader(resourceLoader);
    }

    public PromptTemplateDefinition load(String scenarioCode, String language) {
        String templateText = templateLoader.loadTemplate(scenarioCode, language);
        PromptSlotSchema slotSchema = slotSchemaLoader.loadSlotSchema(scenarioCode, language);
        return new PromptTemplateDefinition(
                scenarioCode, language, templateText, toSlotDefinitions(slotSchema));
    }

    public List<PromptTemplateDefinition> loadAll(List<ScenarioDefinition> scenarios, String language) {
        return scenarios.stream().map(scenario -> load(scenario.scenarioCode(), language)).toList();
    }

    private static List<PromptTemplateSlotDefinition> toSlotDefinitions(PromptSlotSchema slotSchema) {
        return slotSchema.slotDefinitions().stream()
                .map(def -> new PromptTemplateSlotDefinition(def.name(), def.required()))
                .toList();
    }
}
