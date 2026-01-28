package com.buenws.buenws_backend.api.service.tokens;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

@Service
public class TokenService {

    @Value("${jwt.secret}")
    private String secretKeyString;

    public String generateToken(String Username) throws JOSEException {

        Date now = getCurrentDate();
        Date exp = getHourFromNow();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(Username)
                .issuer("https://buenows.org")
                .issueTime(now)
                .expirationTime(exp)
                .claim("roles", List.of("ROLE_USER"))
                .build();

        byte[] sharedSecret = secretKeyString.getBytes(StandardCharsets.UTF_8);
        JWSSigner signer = new MACSigner(sharedSecret);
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), payload);
        jwsObject.sign(signer);

        return jwsObject.serialize();
    }

    public boolean validateToken(String token) throws ParseException, JOSEException{
        JWSObject jwsObject = JWSObject.parse(token);
        JWSVerifier verifier = new MACVerifier(secretKeyString);
        return jwsObject.verify(verifier);
    }

    public String getUsernameFromToken(String token) throws ParseException {
        JWSObject jwsObject = JWSObject.parse(token);
        return jwsObject.getPayload().toJSONObject().get("sub").toString();
    }

    public long getExpirationFromToken(String token) throws ParseException {
        JWSObject jwsObject = JWSObject.parse(token);
        String expString = jwsObject.getPayload().toJSONObject().get("exp").toString();
        return Long.parseLong(expString);
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

}
