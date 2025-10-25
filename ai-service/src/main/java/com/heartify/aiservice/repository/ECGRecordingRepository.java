package com.heartify.aiservice.repository;

import com.heartify.aiservice.entity.ECGRecording;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ECGRecordingRepository extends MongoRepository<ECGRecording, String> {
}
