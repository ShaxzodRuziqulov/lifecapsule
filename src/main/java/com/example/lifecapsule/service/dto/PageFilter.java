package com.example.lifecapsule.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class PageFilter {
    private static final int MAX_PAGE_SIZE = 100;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(MAX_PAGE_SIZE)
    private int size = 20;
    private String q;
    private String sortBy;
    private String direction;

    public Pageable toPageable(String defaultSort, Map<String, String> allowedSorts) {
        String sort = resolveSort(defaultSort);
        String property = allowedSorts.get(sort);
        if (property == null) {
            throw new IllegalArgumentException("sortBy noto'g'ri: " + sort);
        }

        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(resolveDirection()), property)
                .and(Sort.by(Sort.Direction.ASC, "id")));
    }

    public String resolveSort(String defaultSort) {
        return sortBy == null || sortBy.isBlank() ? defaultSort : sortBy.trim();
    }

    public String resolveDirection() {
        if (direction == null || direction.isBlank()) {
            return Sort.Direction.ASC.name().toLowerCase(Locale.ROOT);
        }
        if ("asc".equalsIgnoreCase(direction.trim()) || "desc".equalsIgnoreCase(direction.trim())) {
            return direction.trim().toLowerCase(Locale.ROOT);
        }
        throw new IllegalArgumentException("direction faqat asc yoki desc bo'lishi kerak");
    }

    public String normalizedQuery() {
        return q == null || q.isBlank() ? null : q.trim();
    }
}
