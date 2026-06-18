package net.openan.a2at.sdk.server;

import java.nio.file.Path;
import java.util.Map;
import net.openan.a2at.sdk.server.model.PromptComplianceResult;
import net.openan.a2at.sdk.server.assembly.DefaultA2ATServerBuilder;
import net.openan.a2at.sdk.core.model.A2ATConfig;
import net.openan.a2at.sdk.negotiation.runtime.RoleBoundNegotiationOrchestrator;
import net.openan.a2at.sdk.negotiation.types.model.NegotiationContext;
import net.openan.a2at.sdk.negotiation.types.model.NegotiationStatus;
import net.openan.a2at.sdk.negotiation.types.model.NegotiationType;
import net.openan.a2at.sdk.server.compliance.ServerPromptComplianceOrchestrator;

/**
 * High-level server facade for prompt compliance and negotiation APIs.
 * The caller provides the `.env` file path explicitly, typically after copying the repository `env.example`.
 *
 * @since 2026-06
 */
public final class A2ATServer {

    private final ServerPromptComplianceOrchestrator promptComplianceOrchestrator;

    private final RoleBoundNegotiationOrchestrator negotiationOrchestrator;

    /**
     * Creates a server facade from one user-supplied `.env` path.
     *
     * @param envPath user-supplied `.env` file path
     */
    public A2ATServer(Path envPath) {
        Path resolvedEnvPath = envPath.toAbsolutePath().normalize();
        A2ATConfig config = resolvePromptResourceLocalRootDir(A2ATConfig.load(resolvedEnvPath), resolvedEnvPath);
        DefaultA2ATServerBuilder builder =
                DefaultA2ATServerBuilder.builder().envPath(resolvedEnvPath).config(config);
        this.promptComplianceOrchestrator = builder.buildPromptComplianceOrchestrator();
        this.negotiationOrchestrator = builder.buildNegotiationOrchestrator();
    }

    /**
     * Checks one processed task prompt for server-side compliance.
     *
     * @param processedPromptText processed task prompt text
     * @return compliance result containing only success state or failure details
     */
    public PromptComplianceResult checkTaskPrompt(String processedPromptText) {
        return promptComplianceOrchestrator.checkTaskPrompt(processedPromptText);
    }

    /**
     * Starts a new negotiation payload for the requested negotiation type.
     *
     * @param type negotiation type to initiate
     * @param contentText human-readable negotiation message
     * @param facts structured facts attached to the payload
     * @return transport payload representing the initial negotiation turn
     */
    public Map<String, Object> startNegotiation(NegotiationType type, String contentText, Map<String, Object> facts) {
        return negotiationOrchestrator.startNegotiation(type, contentText, facts);
    }

    /**
     * Processes a received negotiation message using its transport context payload.
     *
     * @param message received negotiation message
     * @param context transport context payload associated with the message
     * @return normalized payload describing the receive result
     */
    public Map<String, Object> receiveNegotiation(String message, Map<String, Object> context) {
        return negotiationOrchestrator.receiveNegotiation(message, context);
    }

    /**
     * Continues an existing negotiation with a locally stored context snapshot.
     *
     * @param context current negotiation context
     * @param status next status to emit
     * @param contentText continuation message content
     * @return transport payload representing the next negotiation turn
     */
    public Map<String, Object> continueNegotiation(
            NegotiationContext context, NegotiationStatus status, String contentText) {
        return negotiationOrchestrator.continueNegotiation(context, status, contentText);
    }

    private A2ATConfig resolvePromptResourceLocalRootDir(A2ATConfig config, Path envPath) {
        String localRootDir = config.prompt().localRootDir();
        Path localRootPath = Path.of(localRootDir);
        Path resolvedLocalRootPath = localRootPath.isAbsolute()
                ? localRootPath.normalize()
                : envPath.getParent().resolve(localRootPath).toAbsolutePath().normalize();
        return new A2ATConfig(
                new net.openan.a2at.sdk.core.model.PromptRuntimeConfig(
                        config.prompt().language(), config.prompt().sourceType(), resolvedLocalRootPath.toString()),
                config.llm(),
                config.negotiation(),
                config.promptCompliance());
    }
}
