package com.buenws.buenws_backend.API.Repository.Repositories;

import com.buenws.buenws_backend.API.Entity.ChatbotMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessageEntity, Long> {
    List<ChatbotMessageEntity> findByConversationConversationIdOrderBySequenceNumberAsc(UUID conversationId);
}
