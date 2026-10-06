/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:09.12.2024
 * TIME:14:06
 */
package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

/**
 * Tokens carry the user id (stable across username changes) and a fingerprint of the current
 * password hash, so changing or resetting a password revokes every token issued before it.
 */
@Service
public class JwtService {
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String PASSWORD_FINGERPRINT_CLAIM = "pwv";

    @Value("${security.jwt.secret-key}")
    private String secretKey;
    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;
    @Value("${security.jwt.refresh-expiration-time}")
    private long refreshExpiration;

    public Long extractUserId(String token) {
        return Long.valueOf(extractAllClaims(token).getSubject());
    }

    public String generateToken(Users user) {
        return buildToken(user, jwtExpiration, "access");
    }

    public String generateRefreshToken(Users user) {
        return buildToken(user, refreshExpiration, "refresh");
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    public boolean isTokenValid(String token, Users user) {
        return isValidForType(token, user, "access");
    }

    public boolean isRefreshTokenValid(String token, Users user) {
        return isValidForType(token, user, "refresh");
    }

    private String buildToken(Users user, long expiration, String tokenType) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .claim(PASSWORD_FINGERPRINT_CLAIM, passwordFingerprint(user))
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isValidForType(String token, Users user, String tokenType) {
        final Claims claims = extractAllClaims(token);
        return tokenType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                && String.valueOf(user.getId()).equals(claims.getSubject())
                && passwordFingerprint(user).equals(claims.get(PASSWORD_FINGERPRINT_CLAIM, String.class))
                && user.isEnabled()
                && user.isAccountNonExpired()
                && claims.getExpiration() != null
                && claims.getExpiration().after(new Date());
    }

    private String passwordFingerprint(Users user) {
        String password = user.getPassword() == null ? "" : user.getPassword();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
    }
}
