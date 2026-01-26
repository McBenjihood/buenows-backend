package com.buenws.buenws_backend.api.repository;

import com.buenws.buenws_backend.api.entities.InquiryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<InquiryEntity, Long> {

    @Query("SELECT i FROM InquiryEntity i WHERE i.email = :email")
    Optional<InquiryEntity> findByEmail(String email);
}
