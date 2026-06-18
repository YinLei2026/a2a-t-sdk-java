package net.openan.a2at.sdk.server.assembly;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import net.openan.a2at.sdk.server.model.PromptTemplateDefinition;
import org.junit.jupiter.api.Test;

class LocalFileServerPromptTemplateLoaderTest {

    @Test
    void loadReadsSlotDefinitionsUsingPromptPackagePublicLoaders() {
        LocalFileServerPromptTemplateLoader loader = new LocalFileServerPromptTemplateLoader(
                Path.of("..", "a2a-t-resources", "src", "main", "resources", "prompt_resources"));

        PromptTemplateDefinition definition = loader.load("energy_saving", "zh-CN");

        assertEquals("energy_saving", definition.scenarioCode());
        assertEquals(4, definition.slotDefinitions().size());
        assertEquals(false, definition.slotDefinitions().get(0).required());
    }
}
