package com.heartify.aiservice.repository;

import com.heartify.aiservice.entity.Explanation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExplanationRepository extends MongoRepository<Explanation, String> {
}
