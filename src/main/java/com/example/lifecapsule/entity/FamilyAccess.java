package com.example.lifecapsule.entity;

import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "family_access",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_family_access_family_user", columnNames = {"family_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_family_access_family", columnList = "family_id"),
                @Index(name = "idx_family_access_user", columnList = "user_id"),
                @Index(name = "idx_family_access_status", columnList = "status"),
                @Index(name = "idx_family_access_role", columnList = "accessRole")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class FamilyAccess extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FamilyAccessRole accessRole = FamilyAccessRole.VIEWER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessStatus status = AccessStatus.ACTIVE;
}
