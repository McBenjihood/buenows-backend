package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.InquiryEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidInquiryException;
import com.buenws.buenws_backend.API.Records.UserRecords;
import com.buenws.buenws_backend.API.Repository.InquiryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    public InquiryService(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    @Transactional
    public UserRecords.ApiResponse<Void> submitContactForm(UserRecords.FormSubmissionRequestRecord formSubmissionRequestRecord) {
        try {
            InquiryEntity inquiry = new InquiryEntity();

            inquiry.setEmail(formSubmissionRequestRecord.email());
            inquiry.setTitle(formSubmissionRequestRecord.title());
            inquiry.setMessage(formSubmissionRequestRecord.message());

            inquiryRepository.save(inquiry);
            return UserRecords.ApiResponse.success("Contact Form was submitted.");
        } catch (Exception e) {
            throw new InvalidInquiryException("Could not submit inquiry to Database", "INVALID_INQUIRY", e);
        }
    }
}