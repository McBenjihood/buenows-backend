package com.buenws.buenws_backend.api.service.tokens;

import com.buenws.buenws_backend.api.entity.RefreshTokenEntity;
import com.buenws.buenws_backend.api.entity.UserEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.ExpiredTokenException;
import com.buenws.buenws_backend.api.exception.customExceptions.InvalidRefreshTokenException;
import com.buenws.buenws_backend.api.exception.customExceptions.ParseTokenException;
import com.buenws.buenws_backend.api.repository.RefreshTokenRepository;
import com.buenws.buenws_backend.api.repository.UserRepository;
import com.buenws.buenws_backend.util.BuenowsUtil;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
public class TokenService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    //Secret Values sources from secrets.properties
    @Value("${jwt.secret}")
    private String tokenSecret;

    //Methods below are for generating JWTToken and RefreshToken
    public String generateJWTToken(UserEntity userEntity) throws JOSEException {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userEntity.getEmail())
                .issuer("https://buenows.org")
                .issueTime(Date.from(BuenowsUtil.getCurrentDate()))
                .expirationTime(Date.from(BuenowsUtil.getHourFromNow()))
                .claim("roles", userEntity.getAuthorities())
                .build();

        byte[] sharedSecret = tokenSecret.getBytes(StandardCharsets.UTF_8);
        JWSSigner signer = new MACSigner(sharedSecret);
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), payload);
        jwsObject.sign(signer);

        return jwsObject.serialize();
    }
    public String generateRefreshToken() {
        byte[] byteArray = new byte[64];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(byteArray);

        return Base64.getEncoder().encodeToString(byteArray);
    }

    //Methods below validate respective token types and return their matching Entities.
    public RefreshTokenEntity validateRefreshToken(String token) throws ParseException, JOSEException {
        Optional<RefreshTokenEntity> refreshTokenOptional = refreshTokenRepository.findByToken(token);
        if (refreshTokenOptional.isPresent()) {
            RefreshTokenEntity refreshTokenEntity = refreshTokenOptional.get();
            if (BuenowsUtil.getCurrentDate().isBefore(refreshTokenEntity.getExpires_at())) {
                return refreshTokenEntity;
            } else {
                throw new ExpiredTokenException("Please Log in again.", "EXPIRED_TOKEN");
            }
        } else {
            throw new InvalidRefreshTokenException("Please Log in again.", "INVALID_TOKEN");
        }
    }
    public Optional<UserEntity> validateJWTToken(String token){
        JWTClaimsSet claimsSet;
        try {
            JWSObject jwsObject = JWSObject.parse(token);
            JWSVerifier verifier = new MACVerifier(tokenSecret.getBytes(StandardCharsets.UTF_8));
            if(!jwsObject.verify(verifier)){
                throw new ParseTokenException("Please Log in again.", "INVALID_TOKEN");
            }
            claimsSet = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());
        }catch (JOSEException | ParseException e) {
            throw new ParseTokenException("Please Log in again.", "INVALID_TOKEN", e);
        }

        //Validating if Token is expired.
        Date currentDate = new Date(System.currentTimeMillis());
        if (currentDate.after(claimsSet.getExpirationTime())){
            throw new ExpiredTokenException("Please Log in again.", "EXPIRED_TOKEN");
        }

        //Returning potential User from DB.
        return userRepository.findByEmail(claimsSet.getSubject());
    }

    //Methods below Parse Details about User from Token
    public String getUsernameFromToken(String token) throws ParseException {
        JWSObject jwsObject = JWSObject.parse(token);
        return jwsObject.getPayload().toJSONObject().get("sub").toString();
    }
    public Date getExpirationFromToken(String token) throws ParseException {
        JWSObject jwsObject = JWSObject.parse(token);
        JWTClaimsSet claimSet = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());
        return claimSet.getExpirationTime();
    }
    public String parseTokenFromHeader(String header){
        String token = "";
        if(header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }
        return token;
    }
}
