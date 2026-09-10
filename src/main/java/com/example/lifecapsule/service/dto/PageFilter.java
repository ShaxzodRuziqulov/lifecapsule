package com.example.lifecapsule.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@Getter
@Setter
public class PageFilter {
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * The client owns the requested page and size. The server only validates
     * them so a manually crafted request cannot create an unsafe query.
     */
    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(MAX_PAGE_SIZE)
    private int size = 20;

    private String q;
    private String sortBy;
    private String direction;

    public String normalizedQuery() {
        return q == null || q.isBlank() ? null : q.trim();
    }

    public Pageable toPageable(String defaultSort, Map<String, String> allowedSorts) {
        String requestedSort = sortBy == null || sortBy.isBlank() ? defaultSort : sortBy;
        String sortProperty = allowedSorts.get(requestedSort);
        if (sortProperty == null) {
            throw new IllegalArgumentException("Sortlash maydoni ruxsat etilmagan: " + requestedSort);
        }

        Sort.Direction sortDirection;
        if (direction == null || direction.isBlank() || "desc".equalsIgnoreCase(direction)) {
            sortDirection = Sort.Direction.DESC;
        } else if ("asc".equalsIgnoreCase(direction)) {
            sortDirection = Sort.Direction.ASC;
        } else {
            throw new IllegalArgumentException("direction faqat asc yoki desc bo'lishi mumkin");
        }

        Sort sort = Sort.by(sortDirection, sortProperty);
        if (!"id".equals(sortProperty)) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }
}
