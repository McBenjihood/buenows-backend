package com.buenws.buenws_backend.api.repository;

import com.buenws.buenws_backend.api.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    public Optional<RefreshTokenEntity> findByToken(String token);
    public Optional<RefreshTokenEntity> findById(UUID id);
}
