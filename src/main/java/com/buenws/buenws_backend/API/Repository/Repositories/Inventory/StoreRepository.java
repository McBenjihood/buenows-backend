package com.buenws.buenws_backend.API.Repository.Repositories.Inventory;

import com.buenws.buenws_backend.API.Entity.Inventory.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<StoreEntity, UUID> {
    Optional<StoreEntity> findByStoreID (UUID uuid)
}
