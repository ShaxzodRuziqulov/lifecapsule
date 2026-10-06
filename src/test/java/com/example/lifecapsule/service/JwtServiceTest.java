package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.entity.enumirated.Status;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService service = new JwtService();
    private final String key = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    private Users user;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "secretKey", key);
        ReflectionTestUtils.setField(service, "jwtExpiration", 60000L);
        ReflectionTestUtils.setField(service, "refreshExpiration", 120000L);
        user = user(7L, "member", "$2a$10$hash");
    }

    @Test
    void tokensCannotBeUsedInterchangeably() {
        String access = service.generateToken(user);
        String refresh = service.generateRefreshToken(user);
        assertTrue(service.isTokenValid(access, user));
        assertFalse(service.isRefreshTokenValid(access, user));
        assertTrue(service.isRefreshTokenValid(refresh, user));
        assertFalse(service.isTokenValid(refresh, user));
        assertEquals(7L, service.extractUserId(access));
    }

    @Test
    void legacyUsernameTokensAreRejected() {
        String legacy = Jwts.builder().setSubject("member").claim("token_type", "access")
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(key)), SignatureAlgorithm.HS256).compact();
        assertFalse(service.isTokenValid(legacy, user));
        assertThrows(NumberFormatException.class, () -> service.extractUserId(legacy));
    }

    @Test
    void renamingTheUserKeepsTheSessionValid() {
        String access = service.generateToken(user);
        user.setUserName("renamed");
        assertTrue(service.isTokenValid(access, user));
    }

    @Test
    void changingThePasswordRevokesEarlierTokens() {
        String access = service.generateToken(user);
        String refresh = service.generateRefreshToken(user);
        user.setPassword("$2a$10$another-hash");
        assertFalse(service.isTokenValid(access, user));
        assertFalse(service.isRefreshTokenValid(refresh, user));
    }

    @Test
    void inactiveAndDifferentUsersAreRejected() {
        String access = service.generateToken(user);
        Users other = user(8L, "member", "$2a$10$hash");
        assertFalse(service.isTokenValid(access, other));
        user.setStatus(Status.INACTIVE);
        assertFalse(service.isTokenValid(access, user));
    }

    private Users user(Long id, String username, String password) {
        Users user = new Users();
        user.setId(id);
        user.setUserName(username);
        user.setPassword(password);
        user.setRole(Role.USER);
        user.setStatus(Status.ACTIVE);
        return user;
    }
}
