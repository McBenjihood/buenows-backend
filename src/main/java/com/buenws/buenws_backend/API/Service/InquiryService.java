package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.InquiryEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidInquiryException;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.Repositories.InquiryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    public InquiryService(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    @Transactional
    public Records.ApiResponse<Void> submitContactForm(Records.FormSubmissionRequest formSubmissionRequest) {
        try {
            InquiryEntity inquiry = new InquiryEntity();

            inquiry.setEmail(formSubmissionRequest.email());
            inquiry.setTitle(formSubmissionRequest.title());
            inquiry.setMessage(formSubmissionRequest.message());

            inquiryRepository.save(inquiry);
            return Records.ApiResponse.success("Contact Form was submitted.");
        } catch (Exception e) {
            throw new InvalidInquiryException("Could not submit inquiry to Database", "INVALID_INQUIRY", e);
        }
    }

    @Transactional(readOnly = true)
    public Records.ApiResponse<List<Records.InquiryResponse>> getAllInquiries() {
        List<Records.InquiryResponse> inquiries = inquiryRepository.findAll().stream()
                .sorted(Comparator.comparing(InquiryEntity::getCreated_at).reversed())
                .map(inquiry -> new Records.InquiryResponse(
                        inquiry.getId(),
                        inquiry.getEmail(),
                        inquiry.getTitle(),
                        inquiry.getMessage(),
                        inquiry.getCreated_at() != null ? inquiry.getCreated_at().toString() : null
                ))
                .toList();

        return Records.ApiResponse.success("Inquiries loaded successfully.", inquiries);
    }
}