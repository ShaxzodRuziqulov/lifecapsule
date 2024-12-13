/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:11.12.2024
 * TIME:12:41
 */
package com.example.lifecapsule.service.response;


import java.util.Objects;


public class LoginResponse {
    private String token;
    private String tokenType;
    private long expiresIn;

    public LoginResponse() {
    }

    public LoginResponse(String token, String tokenType, long expiresIn) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginResponse that = (LoginResponse) o;
        return expiresIn == that.expiresIn && Objects.equals(token, that.token) && Objects.equals(tokenType, that.tokenType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, tokenType, expiresIn);
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "token='" + token + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                '}';
    }
}
