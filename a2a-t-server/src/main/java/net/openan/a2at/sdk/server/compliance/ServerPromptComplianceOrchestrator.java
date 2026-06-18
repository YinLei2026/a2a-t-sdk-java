package net.openan.a2at.sdk.server.compliance;

import net.openan.a2at.sdk.server.model.PromptComplianceResult;

/**
 * Internal server-side prompt-compliance orchestration contract used by the public server facade.
 *
 * @since 2026-06
 */
@FunctionalInterface
public interface ServerPromptComplianceOrchestrator {

    /**
     * Checks one processed task prompt for server-side compliance.
     *
     * @param processedPromptText processed task prompt text
     * @return typed compliance result
     */
    PromptComplianceResult checkTaskPrompt(String processedPromptText);
}
