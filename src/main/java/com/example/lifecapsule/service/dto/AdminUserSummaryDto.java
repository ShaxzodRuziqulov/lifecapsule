package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.entity.enumirated.Status;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminUserSummaryDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private Status status;
    private long familyCount;
    private LocalDateTime createdAt;
}
