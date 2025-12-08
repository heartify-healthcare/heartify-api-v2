package com.heartify.aiservice.rag;

import com.heartify.aiservice.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Service for ingesting and processing Markdown documents
 * Handles text chunking and vectorization for RAG
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final VectorStoreService vectorStoreService;
    
    @Value("${rag.chunk-size:500}")
    private int chunkSize;
    
    @Value("${rag.chunk-overlap:100}")
    private int chunkOverlap;

    // Patterns for splitting text
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\n+");
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern MARKDOWN_HEADER_PATTERN = Pattern.compile("^#{1,6}\\s+.+$", Pattern.MULTILINE);

    public DocumentIngestionService(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Ingest a single Markdown file
     * 
     * @param filePath Path to the Markdown file
     * @return Number of chunks created and stored
     */
    public int ingestMarkdownFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            String fileName = filePath.getFileName().toString();
            
            log.info("Ingesting file: {} ({} characters)", fileName, content.length());
            
            List<String> chunks = splitIntoChunks(content);
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("source", fileName);
            metadata.put("type", "markdown");
            
            int stored = vectorStoreService.upsertDocuments(chunks, metadata);
            
            log.info("Successfully ingested {} chunks from {}", stored, fileName);
            return stored;
            
        } catch (IOException e) {
            throw new AiServiceException("Failed to read file: " + filePath, e);
        }
    }

    /**
     * Ingest all Markdown files from a directory
     * 
     * @param directoryPath Path to the directory containing Markdown files
     * @return Total number of chunks created and stored
     */
    public int ingestDirectory(Path directoryPath) {
        try (Stream<Path> paths = Files.walk(directoryPath)) {
            int totalChunks = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".md"))
                    .mapToInt(this::ingestMarkdownFile)
                    .sum();
            
            log.info("Ingested total of {} chunks from directory: {}", totalChunks, directoryPath);
            return totalChunks;
            
        } catch (IOException e) {
            throw new AiServiceException("Failed to process directory: " + directoryPath, e);
        }
    }

    /**
     * Ingest Markdown content directly (useful for API endpoints)
     * 
     * @param content Markdown content to ingest
     * @param sourceName Source identifier for the content
     * @return Number of chunks created and stored
     */
    public int ingestContent(String content, String sourceName) {
        log.info("Ingesting content from source: {} ({} characters)", sourceName, content.length());
        
        List<String> chunks = splitIntoChunks(content);
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", sourceName);
        metadata.put("type", "markdown");
        
        int stored = vectorStoreService.upsertDocuments(chunks, metadata);
        
        log.info("Successfully ingested {} chunks from {}", stored, sourceName);
        return stored;
    }

    /**
     * Split text into overlapping chunks using recursive character text splitter logic
     * Respects Markdown structure (headers, paragraphs)
     * 
     * @param text The text to split
     * @return List of text chunks
     */
    public List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        
        // First, try to split by Markdown headers to preserve document structure
        List<String> sections = splitByHeaders(text);
        
        for (String section : sections) {
            if (section.length() <= chunkSize) {
                // Section is small enough, use as-is
                if (!section.isBlank()) {
                    chunks.add(section.trim());
                }
            } else {
                // Section is too large, split further
                chunks.addAll(splitLargeSection(section));
            }
        }
        
        // Apply overlap if configured
        if (chunkOverlap > 0 && chunks.size() > 1) {
            chunks = applyOverlap(chunks);
        }
        
        return chunks;
    }

    /**
     * Split text by Markdown headers
     */
    private List<String> splitByHeaders(String text) {
        List<String> sections = new ArrayList<>();
        
        // Split by headers while keeping the headers with their content
        String[] parts = MARKDOWN_HEADER_PATTERN.split(text);
        java.util.regex.Matcher matcher = MARKDOWN_HEADER_PATTERN.matcher(text);
        
        List<String> headers = new ArrayList<>();
        while (matcher.find()) {
            headers.add(matcher.group());
        }
        
        // Combine headers with their following content
        for (int i = 0; i < parts.length; i++) {
            StringBuilder section = new StringBuilder();
            
            if (i > 0 && i - 1 < headers.size()) {
                section.append(headers.get(i - 1)).append("\n");
            }
            section.append(parts[i]);
            
            if (!section.toString().isBlank()) {
                sections.add(section.toString());
            }
        }
        
        // If no headers found, return the whole text as one section
        if (sections.isEmpty() && !text.isBlank()) {
            sections.add(text);
        }
        
        return sections;
    }

    /**
     * Split a large section into smaller chunks
     */
    private List<String> splitLargeSection(String section) {
        List<String> chunks = new ArrayList<>();
        
        // Try splitting by paragraphs first
        String[] paragraphs = PARAGRAPH_PATTERN.split(section);
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() + 2 <= chunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            } else {
                // Save current chunk if not empty
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                
                // If paragraph itself is too large, split by sentences
                if (paragraph.length() > chunkSize) {
                    chunks.addAll(splitBySentences(paragraph));
                } else {
                    currentChunk.append(paragraph);
                }
            }
        }
        
        // Don't forget the last chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }

    /**
     * Split text by sentences when paragraphs are too large
     */
    private List<String> splitBySentences(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = SENTENCE_PATTERN.split(text);
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() + 1 <= chunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                
                // If sentence itself is too large, force split
                if (sentence.length() > chunkSize) {
                    chunks.addAll(forceSplit(sentence));
                } else {
                    currentChunk.append(sentence);
                }
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }

    /**
     * Force split text that's too large even for sentence splitting
     */
    private List<String> forceSplit(String text) {
        List<String> chunks = new ArrayList<>();
        
        for (int i = 0; i < text.length(); i += chunkSize - chunkOverlap) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end).trim());
        }
        
        return chunks;
    }

    /**
     * Apply overlap between chunks to maintain context
     */
    private List<String> applyOverlap(List<String> chunks) {
        if (chunkOverlap <= 0 || chunks.size() <= 1) {
            return chunks;
        }
        
        List<String> overlappedChunks = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            StringBuilder chunk = new StringBuilder();
            
            // Add overlap from previous chunk
            if (i > 0) {
                String prevChunk = chunks.get(i - 1);
                int overlapStart = Math.max(0, prevChunk.length() - chunkOverlap);
                chunk.append(prevChunk.substring(overlapStart)).append(" ");
            }
            
            chunk.append(chunks.get(i));
            overlappedChunks.add(chunk.toString().trim());
        }
        
        return overlappedChunks;
    }
}
