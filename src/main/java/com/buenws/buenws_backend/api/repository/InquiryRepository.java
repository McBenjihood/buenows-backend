package com.buenws.buenws_backend.api.repository;

import com.buenws.buenws_backend.api.entity.InquiryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryRepository extends JpaRepository<InquiryEntity, Long> {
    //Optional<List<InquiryEntity>> findByEmail(String email);
}
