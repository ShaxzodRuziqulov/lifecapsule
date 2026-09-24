package com.example.lifecapsule.service.dto;

import com.example.lifecapsule.entity.enumirated.MediaType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MediaDto {
    private Long id;
    private Long familyId;
    private Long personId;
    private MediaType type;
    private String originalFileName;
    private String contentType;
    private long fileSize;
    private String caption;
    private String url;
    private LocalDateTime createdAt;
}
