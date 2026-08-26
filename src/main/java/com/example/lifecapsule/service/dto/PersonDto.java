package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.Gender;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PersonDto {
    private Long id;
    private Long familyId;
    private String firstName;
    private String lastName;
    private String maidenName;
    private Gender gender;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private String birthPlace;
    private String occupation;
    private String biography;
    private String photoUrl;
    private Long linkedUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}
