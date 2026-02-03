package com.buenws.buenws_backend.api.repository;

import com.buenws.buenws_backend.api.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    public Optional<RefreshTokenEntity> findByToken(String token);
}
