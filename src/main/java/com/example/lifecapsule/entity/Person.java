package com.example.lifecapsule.entity;

import com.example.lifecapsule.entity.enumirated.Gender;
import com.example.lifecapsule.entity.teamplate.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "persons",
        indexes = {
                @Index(name = "idx_person_family", columnList = "family_id"),
                @Index(name = "idx_person_first_name", columnList = "firstName"),
                @Index(name = "idx_person_last_name", columnList = "lastName"),
                @Index(name = "idx_person_birth_date", columnList = "birthDate"),
                @Index(name = "idx_person_linked_user", columnList = "linked_user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Person extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 100)
    private String maidenName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    private LocalDate birthDate;
    private LocalDate deathDate;

    @Column(length = 150)
    private String birthPlace;

    @Column(length = 150)
    private String occupation;

    @Column(columnDefinition = "TEXT")
    private String biography;

    @Column(length = 500)
    private String photoUrl;

    @Column(length = 500)
    private String videoUrl;

    @Column(length = 300)
    private String avatarStoredFileName;

    @Column(length = 300)
    private String avatarOriginalFileName;

    @Column(length = 150)
    private String avatarContentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id")
    private Users linkedUser;
}
