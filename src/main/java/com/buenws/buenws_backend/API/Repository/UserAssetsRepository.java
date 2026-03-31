package com.buenws.buenws_backend.API.Repository;

import com.buenws.buenws_backend.API.Entity.UserAssetEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAssetsRepository extends JpaRepository<UserAssetEntity, Long> {

    @Query("SELECT MAX(a.assetId) FROM UserAssetEntity a WHERE a.user.id = :userId")
    Long findMaxAssetIdByUserId(@Param("userId") UUID userId);

    Optional<UserAssetEntity> findByUserId(UUID userId);
}