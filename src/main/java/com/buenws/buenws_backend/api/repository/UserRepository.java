package com.buenws.buenws_backend.api.repository;
import com.buenws.buenws_backend.api.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository< UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}
