package com.barometer.crm.repository;

import com.barometer.crm.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(String userId);
    long deleteByUserId(String userId);
}
