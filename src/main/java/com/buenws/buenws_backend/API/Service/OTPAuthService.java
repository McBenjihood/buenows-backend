package com.buenws.buenws_backend.API.Service;

import com.buenws.buenws_backend.API.Entity.OTPAuthEntity;
import com.buenws.buenws_backend.API.Exception.Custom.OTPException;
import com.buenws.buenws_backend.API.Exception.Custom.ResetPasswordException;
import com.buenws.buenws_backend.API.Records.Records;
import com.buenws.buenws_backend.API.Repository.Repositories.OTPAuthRepository;
import com.buenws.buenws_backend.API.Repository.RepositoryRetrieval;
import com.buenws.buenws_backend.API.Service.MessageSender.OTPMailSender;
import com.buenws.buenws_backend.API.Service.MessageSender.OTPMessageSender;
import com.buenws.buenws_backend.Util.CryptographyUtil;
import com.buenws.buenws_backend.Util.TimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Component
public class OTPAuthService {

    @Value("${HASH_SALT}")
    private String salt;

    private final OTPMessageSender messageSender;
    private final OTPAuthRepository OTPAuthRepository;
    private final RepositoryRetrieval repositoryRetrieval;


    public OTPAuthService(OTPMailSender messageSender, OTPAuthRepository OTPAuthRepository, RepositoryRetrieval repositoryRetrieval) {
        this.messageSender = messageSender;
        this.OTPAuthRepository = OTPAuthRepository;
        this.repositoryRetrieval = repositoryRetrieval;
    }

    @Transactional
    public boolean requestFactor(Records.RequestOTPRequest requestOTPRequest) {
        String normalizedContact = normalizeContact(requestOTPRequest.contact_information());
        Optional<OTPAuthEntity> resetCodeEntity = OTPAuthRepository.findByContact(normalizedContact);

        OTPAuthEntity otpAuthEntity = resetCodeEntity.orElseGet(
                () -> new OTPAuthEntity(normalizedContact)
        );

        if (otpAuthEntity.getCooldown().isBefore(TimeUtil.getCurrentTime())){
            String newOTP = CryptographyUtil.generateOTP();

            otpAuthEntity.setOtp_code(CryptographyUtil.HashString(newOTP, salt));
            otpAuthEntity.setAttempts(0);
            otpAuthEntity.setActive(true);
            otpAuthEntity.setUpdated_at(TimeUtil.getCurrentTime());
            otpAuthEntity.setExpires_at(TimeUtil.get15MinutesFromNow());


            messageSender.SendMessage(
                    otpAuthEntity.getContact(),
                    newOTP,
                    requestOTPRequest.language()
            );

            OTPAuthRepository.save(otpAuthEntity);

            return true;
        }else {
            throw new OTPException("Please wait 5 Minutes to try again.", "INVALID_OTP");
        }
    }

    @Transactional(noRollbackFor = ResetPasswordException.class)
    public UUID verifyFactor(String contact_information, String factor) {
        OTPAuthEntity OTPAuthEntity = repositoryRetrieval.getResetCodeEntityFromContact_Information(normalizeContact(contact_information));
        int currentAttempts = OTPAuthEntity.getAttempts();

        if (checkOTPAuthEntity(OTPAuthEntity)) {
            OTPAuthEntity.setAttempts(currentAttempts + 1);

            if (Objects.equals(OTPAuthEntity.getOtp_code(), CryptographyUtil.HashString(factor, salt))) {
                UUID verified_token = UUID.randomUUID();
                OTPAuthEntity.setVerified_token(verified_token);

                OTPAuthRepository.save(OTPAuthEntity);
                return verified_token;
            }
        }

        OTPAuthRepository.save(OTPAuthEntity);
        throw new ResetPasswordException("Couldn't verify Validity of OTP ", "INVALID_OTP");
    }

    public void disableFactor(OTPAuthEntity otpAuthEntity) {
        otpAuthEntity.setCooldown(TimeUtil.get5MinutesAfterNow());

        otpAuthEntity.setOtp_code(null);
        otpAuthEntity.setAttempts(4);
        otpAuthEntity.setActive(false);
        otpAuthEntity.setUpdated_at(TimeUtil.get5MinutesBeforeNow());
        otpAuthEntity.setExpires_at(TimeUtil.get5MinutesBeforeNow());
        otpAuthEntity.setVerified_token(null);

        OTPAuthRepository.save(otpAuthEntity);
    }

    @Transactional
    public boolean checkOTPAuthEntity(OTPAuthEntity otpAuthEntity){
        String pleaseWait = "Please wait 5 Minutes to try again.";
        try {
            if (otpAuthEntity.getActive()){
                if(otpAuthEntity.getAttempts() < 4){
                    if (TimeUtil.getCurrentTime().isBefore(otpAuthEntity.getExpires_at())){
                        if (TimeUtil.getCurrentTime().isAfter(otpAuthEntity.getCooldown())){
                            return true;
                        }else{
                            throw new RuntimeException("You tried to often." + pleaseWait);
                        }
                    }else {
                        throw new RuntimeException("Code is expired." + pleaseWait);
                    }
                }else{
                    throw new RuntimeException("No attempts left." + pleaseWait);
                }
            }else {
                throw new RuntimeException("No active Code for this user." + pleaseWait);
            }
        }catch (Exception e){
            disableFactor(otpAuthEntity);
            throw new OTPException(e.getMessage(), "INVALID_OTP");
        }
    }

    private String normalizeContact(String contactInformation) {
        return contactInformation == null ? "" : contactInformation.trim().toLowerCase(Locale.ROOT);
    }
}
