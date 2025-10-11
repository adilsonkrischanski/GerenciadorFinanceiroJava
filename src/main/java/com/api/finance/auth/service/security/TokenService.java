package com.api.finance.auth.service.security;

import com.api.finance.auth.domain.user.UserEntity;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secretKey;

    public String generateToken(UserEntity userEntity) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);

            return JWT.create()
                    .withIssuedAt(this.getIssuedAt())
                    .withClaim("id", userEntity.getEmail())
                    .withClaim("username", userEntity.getUsername())
                    .withSubject(userEntity.getEmail())
                    .withExpiresAt(this.generateExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException jwtCreationException) {
            String errorMessage = "Error while authenticating!";
            throw new RuntimeException(errorMessage);
        }
    }

    public String getUsernameIfTokenIsValid(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);

            return JWT.require(algorithm)
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException jwtVerificationException) {
            return null;
        }
    }

    private Instant generateExpirationDate() {
        LocalDateTime localDateTime = LocalDateTime.now().plusHours(2);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private Instant getIssuedAt() {
        LocalDateTime localDateTime = LocalDateTime.now();
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
