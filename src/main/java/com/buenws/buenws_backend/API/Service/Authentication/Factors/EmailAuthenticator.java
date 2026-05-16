package com.buenws.buenws_backend.API.Service.Authentication.Factors;

import com.buenws.buenws_backend.API.Entity.OTPAuthEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.OTPException;
import com.buenws.buenws_backend.API.Exception.Custom.ResetPasswordException;
import com.buenws.buenws_backend.API.Repository.Repositories.ResetCodeRepository;
import com.buenws.buenws_backend.API.Service.Authentication.MultiFactorAuthenticator;
import com.buenws.buenws_backend.Util.CryptographyUtil;
import com.buenws.buenws_backend.Util.MailUtil;
import com.buenws.buenws_backend.Util.TimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;


@Component
public class EmailAuthenticator implements MultiFactorAuthenticator {

    @Value("${HASH_SALT}")
    private String salt;

    private final MailUtil mailUtil;
    private final ResetCodeRepository resetCodeRepository;

    public EmailAuthenticator(MailUtil mailUtil, ResetCodeRepository resetCodeRepository) {
        this.mailUtil = mailUtil;
        this.resetCodeRepository = resetCodeRepository;
    }

    @Transactional

    public boolean requestFactor(UserEntity user) {

        OTPAuthEntity OTPAuthEntity = user.getResetCodeEntity();
        String plainOTP = CryptographyUtil.generateOTP();

        OTPAuthEntity.setOtp_code(CryptographyUtil.HashString(plainOTP, salt));
        OTPAuthEntity.setAttempts(0);
        OTPAuthEntity.setActive(true);
        OTPAuthEntity.setUpdated_at(TimeUtil.getCurrentTime());
        OTPAuthEntity.setExpires_at(TimeUtil.get15MinutesFromNow());

        mailUtil.SendOTPMail(
                user.getEmail(),
                "Confirming Identity to change your Password.",
                plainOTP,
                user.getFirst_name()
        );

        resetCodeRepository.save(OTPAuthEntity);

        return true;
    }

    @Transactional(noRollbackFor = ResetPasswordException.class)
    public boolean verifyFactor(UserEntity user, String factor) {

        OTPAuthEntity OTPAuthEntity = user.getResetCodeEntity();
        int currentAttempts = OTPAuthEntity.getAttempts();

        if (checkFactorVerification(OTPAuthEntity)) {
            OTPAuthEntity.setAttempts(currentAttempts + 1);

            if (Objects.equals(OTPAuthEntity.getOtp_code(), CryptographyUtil.HashString(factor, salt))) {
                UUID verified_token = UUID.randomUUID();
                OTPAuthEntity.setVerified_token(verified_token);

                resetCodeRepository.save(OTPAuthEntity);
                return true;
            }
        }

        OTPAuthEntity.setCooldown(TimeUtil.get5MinutesAfterNow());

        resetCodeRepository.save(OTPAuthEntity);
        throw new ResetPasswordException("Couldn't verify Validity of OTP ", "INVALID_OTP");
    }

    @Transactional
    public boolean checkFactorVerification(OTPAuthEntity OTPAuthEntity){
        String pleaseWait = "Please wait 5 Minutes to try again.";
        try {
            if (OTPAuthEntity.getActive()){
                if(OTPAuthEntity.getAttempts() < 4){
                    if (TimeUtil.getCurrentTime().isBefore(OTPAuthEntity.getExpires_at())){
                        if (TimeUtil.getCurrentTime().isAfter(OTPAuthEntity.getCooldown())){
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

            OTPAuthEntity.setCooldown(TimeUtil.get5MinutesAfterNow());

            OTPAuthEntity.setOtp_code(null);
            OTPAuthEntity.setAttempts(4);
            OTPAuthEntity.setActive(false);
            OTPAuthEntity.setUpdated_at(TimeUtil.get5MinutesBeforeNow());
            OTPAuthEntity.setExpires_at(TimeUtil.get5MinutesBeforeNow());

            resetCodeRepository.save(OTPAuthEntity);

            throw new OTPException(e.getMessage(), "INVALID_OTP");
        }
    }
}
