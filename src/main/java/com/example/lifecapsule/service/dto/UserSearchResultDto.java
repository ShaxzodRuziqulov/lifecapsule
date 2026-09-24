package com.example.lifecapsule.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deliberately narrow - used to let a family owner find someone to invite by
 * username, so it must never leak email, role, or account status.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserSearchResultDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
}
