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
import java.text.ParseException;
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

        return getString(claimsSet, tokenSecret);
    }
    public String generateRefreshToken(UserEntity userEntity) throws JOSEException {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userEntity.getEmail())
                .issueTime(Date.from(BuenowsUtil.getCurrentDate()))
                .expirationTime(Date.from(BuenowsUtil.getWeekFromNow()))
                .build();

        return getString(claimsSet, refreshTokenSecret);
    }

    private String getString(JWTClaimsSet claimsSet, String secret) throws JOSEException {
        byte[] sharedSecret = secret.getBytes(StandardCharsets.UTF_8);
        JWSSigner signer = new MACSigner(sharedSecret);
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), payload);
        jwsObject.sign(signer);

        return jwsObject.serialize();
    }

    //Methods below validate respective token types and return their matching Entities.
    public Optional<RefreshTokenEntity> validateRefreshToken(String token) throws ParseException, JOSEException {
        return validateRefresh(token);
    }
    public Optional<UserEntity> validateJWTToken(String token) throws ParseException, JOSEException {
        return validateJWT(token,tokenSecret);
    }

    private Optional<UserEntity> validateJWT(String token, String secret) throws ParseException, JOSEException{
        JWSObject jwsObject = JWSObject.parse(token);
        JWSVerifier verifier = new MACVerifier(secret);

        Date currentDate = new Date(System.currentTimeMillis());

        if(!jwsObject.verify(verifier)){
            return Optional.empty();
        }

        JWTClaimsSet claimsSet = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());

        if (currentDate.after(claimsSet.getExpirationTime())){
            throw new ExpiredTokenException("JWTToken has expired");
        }

        return userRepository.findByEmail(claimsSet.getSubject());
    }
    private Optional<RefreshTokenEntity> validateRefresh(String refreshToken){
        try {
            //
            // IMPORTANT TODO:  Before returning tokenEntity, check if token is expired. If expired throw ExpiredTokenException and catch it in UserService so an appropriate response can be made (Will probably be caught by GlobalExceptionhandler, so that needs to be accounted for).
            //
            return refreshTokenRepository.findByToken(refreshToken);
        }catch (Exception e){
            throw new InvalidRefreshTokenException("Not a valid Refreshtoken, Please log in again");
        }
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
