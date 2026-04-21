package com.buenws.buenws_backend.API.Repository;

import com.buenws.buenws_backend.API.Entity.RefreshTokenEntity;
import com.buenws.buenws_backend.API.Entity.ResetCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResetCodeRepository extends JpaRepository<ResetCodeEntity, String> {
    public Optional<ResetCodeEntity> findById(UUID id);
}
