package com.buenws.buenws_backend.api.service.tokens;

import com.buenws.buenws_backend.api.entity.RefreshTokenEntity;
import com.buenws.buenws_backend.api.entity.UserEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.ExpiredTokenException;
import com.buenws.buenws_backend.api.exception.customExceptions.InvalidRefreshTokenException;
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
import java.util.Random;

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
    @Value("${refresh.secret}")
    private String refreshTokenSecret;

    //Methods below are for generating JWTToken and RefreshToken
    public String generateJWTToken(UserEntity userEntity) throws JOSEException {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userEntity.getEmail())
                .issuer("https://buenows.org")
                .issueTime(Date.from(BuenowsUtil.getCurrentDate()))
                .expirationTime(Date.from(BuenowsUtil.getHourFromNow()))
                .claim("roles", userEntity.getAuthorities())
                .build();

        byte[] sharedSecret = refreshTokenSecret.getBytes(StandardCharsets.UTF_8);
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

                throw new ExpiredTokenException("Refresh Token is expired. Pleas log in again.");
            }

        } else {
            throw new InvalidRefreshTokenException("Not a valid Refreshtoken, Please log in again.");
        }
    }

    public Optional<UserEntity> validateJWTToken(String token) throws ParseException, JOSEException {
        JWSObject jwsObject = JWSObject.parse(token);
        JWSVerifier verifier = new MACVerifier(tokenSecret);

        if(!jwsObject.verify(verifier)){
            return Optional.empty();
        }

        JWTClaimsSet claimsSet = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());
        Date currentDate = new Date(System.currentTimeMillis());

        if (currentDate.after(claimsSet.getExpirationTime())){
            throw new ExpiredTokenException("JWTToken has expired");
        }

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

    //Methods below are for getting Date Objects for required Dates in this class.



}
