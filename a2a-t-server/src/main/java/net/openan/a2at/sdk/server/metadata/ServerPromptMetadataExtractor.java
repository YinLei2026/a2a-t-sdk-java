package net.openan.a2at.sdk.server.metadata;

import net.openan.a2at.sdk.server.model.ProcessedPromptMetadata;

/**
 * Extracts typed metadata from a processed task prompt.
 *
 * @since 2026-06
 */
public interface ServerPromptMetadataExtractor {

    /**
     * Extracts typed metadata from one processed task prompt.
     *
     * @param processedPromptText processed task prompt text
     * @return extracted prompt metadata
     */
    ProcessedPromptMetadata extract(String processedPromptText);
}
