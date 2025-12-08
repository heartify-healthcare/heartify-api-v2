package com.heartify.aiservice.rag;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Initializer for RAG components
 * Creates the Qdrant collection on application startup if RAG is enabled
 */
@Component
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagInitializer {

    private static final Logger log = LoggerFactory.getLogger(RagInitializer.class);

    private final VectorStoreService vectorStoreService;

    @Value("${rag.enabled:true}")
    private boolean ragEnabled;

    public RagInitializer(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @PostConstruct
    public void initialize() {
        if (ragEnabled) {
            log.info("Initializing RAG components...");
            try {
                vectorStoreService.createCollectionIfNotExists();
                long docCount = vectorStoreService.getDocumentCount();
                log.info("RAG initialization complete. Collection has {} documents.", docCount);
            } catch (Exception e) {
                log.warn("Failed to initialize RAG components. RAG features may not work correctly: {}", 
                    e.getMessage());
                // Don't fail startup, RAG is optional
            }
        } else {
            log.info("RAG is disabled. Skipping initialization.");
        }
    }
}
