package com.example.lifecapsule.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PageFilter {
    private int page = 0;
    private int size = 20;
    private String q;
    private String sortBy;
    private String direction;
}
