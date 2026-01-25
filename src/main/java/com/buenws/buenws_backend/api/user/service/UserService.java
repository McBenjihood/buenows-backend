package com.buenws.buenws_backend.api.user.service;

import com.buenws.buenws_backend.api.user.entities.InquiryEntity;
import com.buenws.buenws_backend.api.user.records.UserRecords;
import com.buenws.buenws_backend.api.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserRecords.FormSubmissionResponseRecord submitContactForm(UserRecords.FormSubmissionRequestRecord formSubmissionRequestRecord) {

        InquiryEntity inquiry = new InquiryEntity();

        inquiry.setEmail(formSubmissionRequestRecord.email());
        inquiry.setTitle(formSubmissionRequestRecord.title());
        inquiry.setMessage(formSubmissionRequestRecord.message());

        userRepository.save(inquiry);

        return new UserRecords.FormSubmissionResponseRecord(true, "Message sent successfully");
    }
}
