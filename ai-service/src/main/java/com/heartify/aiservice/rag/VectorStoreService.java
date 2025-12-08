package com.heartify.aiservice.rag;

import com.heartify.aiservice.exception.AiServiceException;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

/**
 * Service for interacting with Qdrant Vector Database
 * Handles vector storage, retrieval, and similarity search
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final QdrantClient qdrantClient;
    private final EmbeddingClient embeddingClient;
    
    @Value("${qdrant.collection-name}")
    private String collectionName;
    
    @Value("${qdrant.vector-size}")
    private int vectorSize;

    public VectorStoreService(QdrantClient qdrantClient, EmbeddingClient embeddingClient) {
        this.qdrantClient = qdrantClient;
        this.embeddingClient = embeddingClient;
    }

    /**
     * Initialize the collection if it doesn't exist
     */
    public void createCollectionIfNotExists() {
        try {
            // Check if collection exists
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            
            if (!exists) {
                log.info("Creating Qdrant collection: {}", collectionName);
                
                qdrantClient.createCollectionAsync(
                    collectionName,
                    Collections.VectorParams.newBuilder()
                        .setDistance(Collections.Distance.Cosine)
                        .setSize(vectorSize)
                        .build()
                ).get();
                
                log.info("Successfully created collection: {}", collectionName);
            } else {
                log.info("Collection already exists: {}", collectionName);
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("Failed to create/check collection: " + e.getMessage(), e);
        }
    }

    /**
     * Upsert document chunks into the vector store
     * 
     * @param chunks List of text chunks to store
     * @param metadata Optional metadata for each chunk (e.g., source document name)
     * @return Number of points successfully stored
     */
    public int upsertDocuments(List<String> chunks, Map<String, String> metadata) {
        try {
            List<Points.PointStruct> points = new ArrayList<>();
            
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                List<Float> embedding = embeddingClient.generateEmbedding(chunk);
                
                Points.PointStruct.Builder pointBuilder = Points.PointStruct.newBuilder()
                        .setId(id(UUID.randomUUID()))
                        .setVectors(vectors(embedding))
                        .putPayload("content", value(chunk))
                        .putPayload("chunk_index", value(i));
                
                // Add metadata if provided
                if (metadata != null) {
                    for (Map.Entry<String, String> entry : metadata.entrySet()) {
                        pointBuilder.putPayload(entry.getKey(), value(entry.getValue()));
                    }
                }
                
                points.add(pointBuilder.build());
                
                log.debug("Created point for chunk {} with {} dimensional vector", i, embedding.size());
            }
            
            // Upsert points to Qdrant
            Points.UpdateResult result = qdrantClient.upsertAsync(collectionName, points).get();
            
            log.info("Upserted {} documents to collection: {}", chunks.size(), collectionName);
            return chunks.size();
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("Failed to upsert documents: " + e.getMessage(), e);
        }
    }

    /**
     * Upsert document chunks without metadata
     */
    public int upsertDocuments(List<String> chunks) {
        return upsertDocuments(chunks, null);
    }

    /**
     * Search for similar documents using text query
     * 
     * @param queryText The query text to search for
     * @param limit Maximum number of results to return
     * @return List of relevant text chunks with their scores
     */
    public List<SearchResult> search(String queryText, int limit) {
        try {
            // Generate embedding for the query
            List<Float> queryVector = embeddingClient.generateQueryEmbedding(queryText);
            
            return searchByVector(queryVector, limit);
            
        } catch (Exception e) {
            throw new AiServiceException("Failed to search documents: " + e.getMessage(), e);
        }
    }

    /**
     * Search for similar documents using pre-computed vector
     * 
     * @param queryVector The query embedding vector
     * @param limit Maximum number of results to return
     * @return List of relevant text chunks with their scores
     */
    public List<SearchResult> searchByVector(List<Float> queryVector, int limit) {
        try {
            List<Points.ScoredPoint> scoredPoints = qdrantClient.searchAsync(
                Points.SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(queryVector)
                    .setLimit(limit)
                    .setWithPayload(enable(true))
                    .build()
            ).get();

            List<SearchResult> results = new ArrayList<>();
            
            for (Points.ScoredPoint point : scoredPoints) {
                String content = point.getPayloadOrDefault("content", value("")).getStringValue();
                float score = point.getScore();
                
                // Extract additional metadata if present
                String source = point.getPayloadOrDefault("source", value("")).getStringValue();
                
                results.add(new SearchResult(content, score, source));
            }
            
            log.debug("Found {} relevant documents for query", results.size());
            return results;
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("Failed to search by vector: " + e.getMessage(), e);
        }
    }

    /**
     * Get the count of documents in the collection
     */
    public long getDocumentCount() {
        try {
            Collections.CollectionInfo info = qdrantClient.getCollectionInfoAsync(collectionName).get();
            return info.getPointsCount();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("Failed to get document count: " + e.getMessage(), e);
        }
    }

    /**
     * Delete all documents from the collection
     */
    public void clearCollection() {
        try {
            qdrantClient.deleteCollectionAsync(collectionName).get();
            createCollectionIfNotExists();
            log.info("Cleared collection: {}", collectionName);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("Failed to clear collection: " + e.getMessage(), e);
        }
    }

    /**
     * Record class for search results
     */
    public record SearchResult(String content, float score, String source) {
        
        @Override
        public String toString() {
            return String.format("[Score: %.4f] %s", score, 
                content.length() > 100 ? content.substring(0, 100) + "..." : content);
        }
    }
}
