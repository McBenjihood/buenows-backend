package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.InquiryEntity;
import com.buenws.buenws_backend.API.Exception.Custom.InvalidInquiryException;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.Repositories.InquiryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            if(formSubmissionRequest.message().length() <= 2000){
                inquiry.setMessage(formSubmissionRequest.message());
            }else {
                throw new InvalidInquiryException("Message is too long.", "INVALID_INQUIRY");
            }


            inquiryRepository.save(inquiry);
            return Records.ApiResponse.success("Contact Form was submitted.");
        } catch (Exception e) {
            throw new InvalidInquiryException("Could not submit inquiry.", "INVALID_INQUIRY", e);
        }
    }

    @Transactional(readOnly = true)
    public Records.ApiResponse<List<Records.InquiryResponse>> getAllInquiries(int page, int size) {
        PageRequest pageRequest = PageRequest.of(safePage(page), safeSize(size), Sort.by(Sort.Direction.DESC, "created_at"));
        List<Records.InquiryResponse> inquiries = inquiryRepository.findAll(pageRequest).stream()
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

    @Transactional
    public Records.ApiResponse<Void> deleteInquiryForAdmin(Long inquiryId) {
        InquiryEntity inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InvalidInquiryException("Inquiry not found.", "INVALID_INQUIRY"));

        inquiryRepository.delete(inquiry);

        return Records.ApiResponse.success("Inquiry deleted successfully.");
    }

    private int safePage(int page) {
        return Math.max(0, page);
    }

    private int safeSize(int size) {
        return Math.min(100, Math.max(1, size));
    }
}
