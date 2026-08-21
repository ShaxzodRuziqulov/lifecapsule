/**
 * Author: Shaxzod Ro'ziqulov
 * User:Ruzikulov
 * DATE:08.12.2024
 * TIME:20:01
 */
package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.entity.enumirated.Status;



public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private Status status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
