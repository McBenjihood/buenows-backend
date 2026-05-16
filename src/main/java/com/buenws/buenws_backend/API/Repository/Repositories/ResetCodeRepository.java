package com.buenws.buenws_backend.API.Repository.Repositories;

import com.buenws.buenws_backend.API.Entity.OTPAuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResetCodeRepository extends JpaRepository<OTPAuthEntity, String> {
    @Query("SELECT r FROM OTPAuthEntity r WHERE r.verified_token = :verifiedToken")
    Optional<OTPAuthEntity> findByVerifiedToken(@Param("verifiedToken") UUID verifiedToken);
}
