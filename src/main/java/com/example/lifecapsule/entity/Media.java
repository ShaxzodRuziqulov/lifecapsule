package com.example.lifecapsule.entity;

import com.example.lifecapsule.entity.enumirated.MediaType;
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
        name = "media",
        indexes = {
                @Index(name = "idx_media_person", columnList = "person_id"),
                @Index(name = "idx_media_family", columnList = "family_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Media extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @Column(nullable = false, length = 300)
    private String storedFileName;

    @Column(nullable = false, length = 300)
    private String originalFileName;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(length = 300)
    private String caption;
}
