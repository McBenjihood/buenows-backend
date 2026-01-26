package com.buenws.buenws_backend.api.service;

import com.buenws.buenws_backend.api.entities.InquiryEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.InvalidInquiryException;
import com.buenws.buenws_backend.api.records.UserRecords;
import com.buenws.buenws_backend.api.repository.InquiryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InquiryService {

    private InquiryRepository inquiryRepository;

    public InquiryService(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    @Transactional
    public UserRecords.FormSubmissionResponseRecord submitContactForm(UserRecords.FormSubmissionRequestRecord formSubmissionRequestRecord) {
        try {
            InquiryEntity inquiry = new InquiryEntity();

            inquiry.setEmail(formSubmissionRequestRecord.email());
            inquiry.setTitle(formSubmissionRequestRecord.title());
            inquiry.setMessage(formSubmissionRequestRecord.message());

            inquiryRepository.save(inquiry);
        } catch (Exception e) {
            System.out.println("Error submitting inquiry to database: " + e.getMessage());
            throw new InvalidInquiryException("Could not submit inquiry to Database");
        }

        return new UserRecords.FormSubmissionResponseRecord(true, "Inquiry submitted successfully");
    }

}