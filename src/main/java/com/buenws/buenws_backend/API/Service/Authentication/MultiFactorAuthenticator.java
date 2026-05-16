package com.buenws.buenws_backend.API.Service.Authentication;

import com.buenws.buenws_backend.API.Entity.OTPAuthEntity;
import com.buenws.buenws_backend.API.Entity.UserEntity;

public interface MultiFactorAuthenticator {
    public boolean requestFactor(UserEntity user);
    public boolean verifyFactor(UserEntity user, String factor);
    public boolean checkFactorVerification(OTPAuthEntity OTPAuthEntity);
}
