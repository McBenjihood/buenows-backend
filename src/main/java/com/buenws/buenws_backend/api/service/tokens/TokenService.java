package com.buenws.buenws_backend.api.service.tokens;

import com.buenws.buenws_backend.api.entity.UserEntity;
import com.buenws.buenws_backend.api.exception.customExceptions.ExpiredTokenException;
import com.buenws.buenws_backend.api.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TokenService {

    @Autowired
    UserRepository userRepository;

    @Value("${jwt.secret}")
    private String tokenSecret;

    @Value("${refresh.secret}")
    private String refreshTokenSecret;

    public String generateToken(UserEntity userEntity) throws JOSEException {

        Date now = getCurrentDate();
        Date exp = getHourFromNow();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userEntity.getEmail())
                .issuer("https://buenows.org")
                .issueTime(now)
                .expirationTime(exp)
                .claim("roles", userEntity.getAuthorities())
                .build();

        return getString(claimsSet, tokenSecret);
    }

    public String generateRefreshToken(UserEntity userEntity) throws JOSEException {
        Date now = getCurrentDate();
        Date exp = getWeekFromNow();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userEntity.getEmail())
                .issueTime(now)
                .expirationTime(exp)
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

    public Optional<UserEntity> validateRefreshToken(String token) throws ParseException, JOSEException {
        return validateToken(token,refreshTokenSecret, "Refresh Token");
    }

    public Optional<UserEntity> validateToken(String token) throws ParseException, JOSEException {
        return validateToken(token,tokenSecret, "Token");
    }

    private Optional<UserEntity> validateToken(String token, String secret, String tokenType) throws ParseException, JOSEException{
        JWSObject jwsObject = JWSObject.parse(token);
        JWSVerifier verifier = new MACVerifier(secret);

        Date currentDate = new Date(System.currentTimeMillis());

        if(!jwsObject.verify(verifier)){
            return Optional.empty();
        }

        JWTClaimsSet claimsSet = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());

        if (currentDate.after(claimsSet.getExpirationTime())){
            throw new ExpiredTokenException(tokenType + " has expired");
        }

        return userRepository.findByEmail(claimsSet.getSubject());
    }


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

    private Date getCurrentDate(){
        return new Date(System.currentTimeMillis());
    }
    private Date getHourFromNow(){
        return new Date(System.currentTimeMillis() +(3600 * 1000));
    }
    private  Date getWeekFromNow(){
        return new Date(System.currentTimeMillis() + (604800 * 1000));
    }


}
