package com.buenws.buenws_backend.API.Repository.Repositories;

import com.buenws.buenws_backend.API.Entity.ChatbotRateLimitEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ChatbotRateLimitRepository extends JpaRepository<ChatbotRateLimitEntity, String> {
    @Modifying
    @Query(value = """
            insert into chatbot_rate_limits (limit_key, request_count, reset_at)
            values (:limitKey, 0, :resetAt)
            on conflict (limit_key) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("limitKey") String limitKey, @Param("resetAt") Instant resetAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rateLimit from ChatbotRateLimitEntity rateLimit where rateLimit.limitKey = :limitKey")
    Optional<ChatbotRateLimitEntity> findByLimitKeyForUpdate(@Param("limitKey") String limitKey);

    long deleteByResetAtBefore(Instant instant);
}
