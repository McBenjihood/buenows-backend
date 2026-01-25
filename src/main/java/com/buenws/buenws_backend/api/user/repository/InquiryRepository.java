package com.buenws.buenws_backend.api.user.repository;

import com.buenws.buenws_backend.api.user.entities.InquiryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<InquiryEntity, Long> {
    Optional<InquiryEntity> findByEmail(String email);
}
