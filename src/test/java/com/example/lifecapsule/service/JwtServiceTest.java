package com.example.lifecapsule.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService service = new JwtService();
    private final String key = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    private final UserDetails user = User.withUsername("member").password("unused").authorities("USER").build();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "secretKey", key);
        ReflectionTestUtils.setField(service, "jwtExpiration", 60000L);
        ReflectionTestUtils.setField(service, "refreshExpiration", 120000L);
    }

    @Test
    void tokensCannotBeUsedInterchangeably() {
        String access = service.generateToken(user);
        String refresh = service.generateRefreshToken(user);
        assertTrue(service.isTokenValid(access, user));
        assertFalse(service.isRefreshTokenValid(access, user));
        assertTrue(service.isRefreshTokenValid(refresh, user));
        assertFalse(service.isTokenValid(refresh, user));
    }

    @Test
    void legacyTokensWithoutTypeAreRejected() {
        String legacy = Jwts.builder().setSubject(user.getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(key)), SignatureAlgorithm.HS256).compact();
        assertFalse(service.isTokenValid(legacy, user));
        assertFalse(service.isRefreshTokenValid(legacy, user));
    }

    @Test
    void disabledAndDifferentUsersAreRejected() {
        UserDetails disabled = User.withUserDetails(user).disabled(true).build();
        UserDetails other = User.withUserDetails(user).username("other").build();
        assertFalse(service.isTokenValid(service.generateToken(user), disabled));
        assertFalse(service.isRefreshTokenValid(service.generateRefreshToken(user), disabled));
        assertFalse(service.isTokenValid(service.generateToken(user), other));
    }
}
