package com.buenws.buenws_backend.API.Service.Authentication.Factors;

import com.buenws.buenws_backend.API.Entity.ResetCodeEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;
import com.buenws.buenws_backend.API.Exception.Custom.ResetPasswordException;
import com.buenws.buenws_backend.API.Records.Records;
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

        ResetCodeEntity resetCodeEntity = user.getResetCodeEntity();
        String plainOTP = CryptographyUtil.generateOTP();

        resetCodeEntity.setReset_code(CryptographyUtil.HashString(plainOTP, salt));
        resetCodeEntity.setActive(true);
        resetCodeEntity.setUpdated_at(TimeUtil.getCurrentTime());
        resetCodeEntity.setExpires_at(TimeUtil.get15MinutesFromNow());
        resetCodeEntity.setAttempts(0);

        mailUtil.SendOTPMail(
                user.getEmail(),
                "Confirming Identity to change your Password.",
                plainOTP,
                user.getFirst_name()
        );

        resetCodeRepository.save(resetCodeEntity);

        return true;
    }

    @Transactional(noRollbackFor = ResetPasswordException.class)
    public boolean verifyFactor(UserEntity user, String factor) {

        ResetCodeEntity resetCodeEntity = user.getResetCodeEntity();
        int currentAttempts = resetCodeEntity.getAttempts();

        if (TimeUtil.getCurrentTime().isBefore(resetCodeEntity.getExpires_at()) && currentAttempts < 4) {
            resetCodeEntity.setAttempts(currentAttempts + 1);

            if (resetCodeEntity.getActive() && Objects.equals(resetCodeEntity.getReset_code(), CryptographyUtil.HashString(factor, salt))) {
                UUID verified_token = UUID.randomUUID();
                resetCodeEntity.setVerified_token(verified_token);

                resetCodeRepository.save(resetCodeEntity);
                return true;
            }
        } else {
            resetCodeEntity.setActive(false);
        }

        resetCodeRepository.save(resetCodeEntity);
        throw new ResetPasswordException("Couldn't verify Validity of OTP ", "INVALID_OTP");
    }
}
