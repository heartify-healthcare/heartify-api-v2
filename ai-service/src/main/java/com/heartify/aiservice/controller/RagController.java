package com.heartify.aiservice.controller;

import com.heartify.aiservice.rag.DocumentIngestionService;
import com.heartify.aiservice.rag.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for RAG Document Management
 * Provides endpoints for ingesting and managing medical knowledge documents
 */
@RestController
@RequestMapping("/api/v1/rag")
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true", matchIfMissing = true)
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final DocumentIngestionService documentIngestionService;
    private final VectorStoreService vectorStoreService;

    public RagController(DocumentIngestionService documentIngestionService,
                        VectorStoreService vectorStoreService) {
        this.documentIngestionService = documentIngestionService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Ingest a Markdown file into the knowledge base
     * 
     * @param file Markdown file to ingest
     * @return Response with number of chunks created
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingestFile(@RequestParam("file") MultipartFile file) {
        log.info("Ingesting file: {}", file.getOriginalFilename());
        
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String sourceName = file.getOriginalFilename() != null ? 
                    file.getOriginalFilename() : "uploaded-document";
            
            int chunksCreated = documentIngestionService.ingestContent(content, sourceName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document ingested successfully");
            response.put("source", sourceName);
            response.put("chunks_created", chunksCreated);
            response.put("total_documents", vectorStoreService.getDocumentCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to ingest file: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to ingest document: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Ingest Markdown content directly via JSON
     * 
     * @param request Request containing content and source name
     * @return Response with number of chunks created
     */
    @PostMapping("/ingest/content")
    public ResponseEntity<Map<String, Object>> ingestContent(@RequestBody IngestContentRequest request) {
        log.info("Ingesting content from source: {}", request.sourceName());
        
        try {
            int chunksCreated = documentIngestionService.ingestContent(
                    request.content(), 
                    request.sourceName()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Content ingested successfully");
            response.put("source", request.sourceName());
            response.put("chunks_created", chunksCreated);
            response.put("total_documents", vectorStoreService.getDocumentCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to ingest content: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to ingest content: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Search the knowledge base
     * 
     * @param request Search request containing query and limit
     * @return List of matching documents with scores
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody SearchRequest request) {
        log.info("Searching knowledge base: {}", request.query());
        
        try {
            int limit = request.limit() != null ? request.limit() : 5;
            List<VectorStoreService.SearchResult> results = vectorStoreService.search(request.query(), limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", request.query());
            response.put("results_count", results.size());
            response.put("results", results.stream().map(r -> Map.of(
                    "content", r.content(),
                    "score", r.score(),
                    "source", r.source()
            )).toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Search failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get knowledge base statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total_documents", vectorStoreService.getDocumentCount());
            stats.put("rag_enabled", true);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get stats: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Clear all documents from the knowledge base
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearKnowledgeBase() {
        log.warn("Clearing knowledge base...");
        
        try {
            vectorStoreService.clearCollection();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Knowledge base cleared successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to clear knowledge base: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to clear knowledge base: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Request/Response records
    public record IngestContentRequest(String content, String sourceName) {}
    public record SearchRequest(String query, Integer limit) {}
}
