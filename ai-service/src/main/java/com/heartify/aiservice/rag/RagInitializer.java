package com.heartify.aiservice.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Initializer for RAG components
 * Creates the Qdrant collection on application startup if RAG is enabled
 * Automatically ingests documents from the knowledge-base folder if the collection is empty
 */
@Component
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagInitializer {

    private static final Logger log = LoggerFactory.getLogger(RagInitializer.class);
    private static final String KNOWLEDGE_BASE_PATH = "classpath:knowledge-base/*.md";

    private final VectorStoreService vectorStoreService;
    private final DocumentIngestionService documentIngestionService;

    @Value("${rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${rag.chunk-size:500}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:100}")
    private int chunkOverlap;

    public RagInitializer(VectorStoreService vectorStoreService,
                          DocumentIngestionService documentIngestionService) {
        this.vectorStoreService = vectorStoreService;
        this.documentIngestionService = documentIngestionService;
    }

    /**
     * Initialize RAG components when the application is fully ready
     * This ensures all beans are available before we start the initialization
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!ragEnabled) {
            log.info("RAG is disabled. Skipping initialization.");
            return;
        }

        log.info("Initializing RAG components...");
        
        try {
            // Step 1: Ensure the collection exists
            vectorStoreService.createCollectionIfNotExists();
            
            // Step 2: Check if collection is empty and needs ingestion
            long docCount = vectorStoreService.getDocumentCount();
            log.info("Current document count in vector store: {}", docCount);
            
            if (docCount == 0) {
                log.info("Vector store is empty. Starting automatic document ingestion...");
                ingestKnowledgeBaseDocuments();
            } else {
                log.info("RAG initialization complete. Collection already has {} documents.", docCount);
            }
            
        } catch (Exception e) {
            log.warn("Failed to initialize RAG components. RAG features may not work correctly: {}", 
                e.getMessage());
            // Don't fail startup, RAG is optional
        }
    }

    /**
     * Ingest all Markdown documents from the knowledge-base folder in classpath
     */
    private void ingestKnowledgeBaseDocuments() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(KNOWLEDGE_BASE_PATH);
            
            if (resources.length == 0) {
                log.info("No Markdown files found in knowledge-base folder. Skipping ingestion.");
                return;
            }
            
            log.info("Found {} Markdown files to ingest from knowledge-base", resources.length);
            
            int totalChunks = 0;
            int filesProcessed = 0;
            
            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    if (filename == null || !filename.toLowerCase().endsWith(".md")) {
                        continue;
                    }
                    
                    String content = readResourceContent(resource);
                    
                    if (content.isBlank()) {
                        log.warn("Skipping empty file: {}", filename);
                        continue;
                    }
                    
                    log.info("Ingesting file: {} ({} characters)", filename, content.length());
                    
                    // Split content into chunks
                    List<String> chunks = documentIngestionService.splitIntoChunks(content);
                    
                    if (chunks.isEmpty()) {
                        log.warn("No chunks created from file: {}", filename);
                        continue;
                    }
                    
                    // Prepare metadata
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("source", filename);
                    metadata.put("type", "markdown");
                    
                    // Upsert to vector store
                    int storedChunks = vectorStoreService.upsertDocuments(chunks, metadata);
                    totalChunks += storedChunks;
                    filesProcessed++;
                    
                    log.info("Successfully ingested {} chunks from {}", storedChunks, filename);
                    
                } catch (Exception e) {
                    log.error("Failed to ingest file: {}. Error: {}", 
                        resource.getFilename(), e.getMessage());
                    // Continue with other files
                }
            }
            
            log.info("=== Document Ingestion Complete ===");
            log.info("Files processed: {}", filesProcessed);
            log.info("Total chunks stored: {}", totalChunks);
            log.info("Final document count: {}", vectorStoreService.getDocumentCount());
            
        } catch (IOException e) {
            log.error("Failed to scan knowledge-base folder: {}", e.getMessage());
        }
    }

    /**
     * Read content from a Spring Resource
     */
    private String readResourceContent(Resource resource) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Force re-ingestion of all documents (useful for development/testing)
     * Can be triggered via Actuator endpoint or programmatically
     */
    public void forceReIngestion() {
        log.info("Forcing re-ingestion of all documents...");
        try {
            vectorStoreService.clearCollection();
            ingestKnowledgeBaseDocuments();
        } catch (Exception e) {
            log.error("Failed to force re-ingestion: {}", e.getMessage());
            throw new RuntimeException("Failed to force re-ingestion", e);
        }
    }

    /**
     * Get current ingestion status
     */
    public Map<String, Object> getIngestionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("ragEnabled", ragEnabled);
        status.put("chunkSize", chunkSize);
        status.put("chunkOverlap", chunkOverlap);
        
        try {
            status.put("documentCount", vectorStoreService.getDocumentCount());
            status.put("collectionExists", true);
        } catch (Exception e) {
            status.put("documentCount", 0);
            status.put("collectionExists", false);
            status.put("error", e.getMessage());
        }
        
        return status;
    }
}
