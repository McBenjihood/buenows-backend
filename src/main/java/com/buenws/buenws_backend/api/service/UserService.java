package com.buenws.buenws_backend.api.service;

import com.buenws.buenws_backend.api.entities.InquiryEntity;
import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.repository.InquiryRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final InquiryRepository inquiryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(InquiryRepository inquiryRepository, PasswordEncoder passwordEncoder) {
        this.inquiryRepository = inquiryRepository;
        this.passwordEncoder = passwordEncoder;
    }


}
