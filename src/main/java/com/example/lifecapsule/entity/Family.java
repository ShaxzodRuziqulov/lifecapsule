package com.example.lifecapsule.entity;

import com.example.lifecapsule.entity.enumirated.FamilyVisibility;
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

@Entity
@Table(
        name = "family",
        indexes = {
                @Index(name = "idx_family_created_by", columnList = "created_by"),
                @Index(name = "idx_family_name", columnList = "name"),
                @Index(name = "idx_family_visibility", columnList = "visibility")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Family extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FamilyVisibility visibility = FamilyVisibility.INVITE_ONLY;

    @Column(length = 300)
    private String coverStoredFileName;

    @Column(length = 300)
    private String coverOriginalFileName;

    @Column(length = 150)
    private String coverContentType;

}
