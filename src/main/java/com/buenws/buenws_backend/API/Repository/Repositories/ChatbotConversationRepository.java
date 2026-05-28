package com.buenws.buenws_backend.API.Repository.Repositories;

import com.buenws.buenws_backend.API.Entity.ChatbotConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotConversationRepository extends JpaRepository<ChatbotConversationEntity, UUID> {
    Optional<ChatbotConversationEntity> findBySessionId(UUID sessionId);
    List<ChatbotConversationEntity> findByCompanyKeyAndCreatedAtAfterOrderByCreatedAtDesc(String companyKey, Instant createdAfter);
    long deleteByExpiresAtBefore(Instant instant);
}
