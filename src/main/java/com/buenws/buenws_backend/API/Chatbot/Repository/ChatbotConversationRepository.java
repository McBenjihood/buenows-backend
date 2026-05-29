package com.buenws.buenws_backend.API.Chatbot.Repository;

import com.buenws.buenws_backend.API.Chatbot.Entity.ChatbotConversationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotConversationRepository extends JpaRepository<ChatbotConversationEntity, UUID> {
    Optional<ChatbotConversationEntity> findBySessionId(UUID sessionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select conversation from ChatbotConversationEntity conversation where conversation.sessionId = :sessionId")
    Optional<ChatbotConversationEntity> findBySessionIdForUpdate(@Param("sessionId") UUID sessionId);
    Page<ChatbotConversationEntity> findByCompanyKeyAndCreatedAtAfter(String companyKey, Instant createdAfter, Pageable pageable);
    long deleteByExpiresAtBefore(Instant instant);
}
