package net.openan.a2at.sdk.client.prompt.recognition;

import java.nio.file.Path;
import java.util.List;
import net.openan.a2at.sdk.prompt.resources.loader.LocalFilePromptScenarioCatalogLoader;
import net.openan.a2at.sdk.prompt.resources.model.ScenarioDefinition;

/**
 * Loads scenario catalogs from one local prompt resource root.
 *
 * @since 2026-06
 */
public final class LocalFileClientScenarioCatalogLoader {
    private final LocalFilePromptScenarioCatalogLoader delegate;

    /**
     * Creates one local scenario catalog loader.
     *
     * @param promptRootDir prompt resource root directory
     */
    public LocalFileClientScenarioCatalogLoader(Path promptRootDir) {
        this.delegate = new LocalFilePromptScenarioCatalogLoader(promptRootDir);
    }

    /**
     * Loads scenario definitions for one language.
     *
     * @param language resource language
     * @return parsed scenario definitions
     */
    public List<ScenarioDefinition> load(String language) {
        return delegate.load(language);
    }
}
