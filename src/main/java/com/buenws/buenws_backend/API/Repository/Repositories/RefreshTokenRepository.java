package com.buenws.buenws_backend.API.Repository.Repositories;

import com.buenws.buenws_backend.API.Entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    public Optional<RefreshTokenEntity> findByToken(String token);
}
