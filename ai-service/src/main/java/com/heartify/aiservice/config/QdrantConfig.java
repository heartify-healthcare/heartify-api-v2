package com.heartify.aiservice.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Qdrant Vector Database client
 */
@Configuration
public class QdrantConfig {

    private static final Logger log = LoggerFactory.getLogger(QdrantConfig.class);

    @Value("${qdrant.host}")
    private String qdrantHost;
    
    @Value("${qdrant.port}")
    private int qdrantPort;

    /**
     * Create Qdrant client bean
     * Uses gRPC for efficient communication
     */
    @Bean
    public QdrantClient qdrantClient() {
        log.info("Initializing Qdrant client: {}:{}", qdrantHost, qdrantPort);
        
        QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false)
                .build();
        
        return new QdrantClient(grpcClient);
    }
}
