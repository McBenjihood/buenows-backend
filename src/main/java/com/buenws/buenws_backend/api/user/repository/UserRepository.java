package com.buenws.buenws_backend.api.user.repository;
import com.buenws.buenws_backend.api.user.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository< UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
}
