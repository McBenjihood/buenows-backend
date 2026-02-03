package com.buenws.buenws_backend.api.repository;

import com.buenws.buenws_backend.api.entity.InquiryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<InquiryEntity, Long> {
    Optional<List<InquiryEntity>> findByEmail(String email);
}
