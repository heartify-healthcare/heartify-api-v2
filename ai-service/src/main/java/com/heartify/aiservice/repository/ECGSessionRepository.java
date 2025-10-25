package com.heartify.aiservice.repository;

import com.heartify.aiservice.entity.ECGSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ECGSessionRepository extends MongoRepository<ECGSession, String> {
    Page<ECGSession> findByUserId(Long userId, Pageable pageable);
}
