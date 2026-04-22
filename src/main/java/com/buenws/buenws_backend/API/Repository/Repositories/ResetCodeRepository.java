package com.buenws.buenws_backend.API.Repository.Repositories;

import com.buenws.buenws_backend.API.Entity.ResetCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResetCodeRepository extends JpaRepository<ResetCodeEntity, String> {
    @Query("SELECT r FROM ResetCodeEntity r WHERE r.verified_token = :verifiedToken")
    Optional<ResetCodeEntity> findByVerifiedToken(@Param("verifiedToken") String verifiedToken);
}
