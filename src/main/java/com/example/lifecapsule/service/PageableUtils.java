package com.example.lifecapsule.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

final class PageableUtils {
    private static final int MAX_PAGE_SIZE = 100;

    private PageableUtils() {
    }

    static Pageable create(
            int page,
            int size,
            String sortBy,
            String direction,
            String defaultSort,
            Map<String, String> allowedSorts
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("page 0 dan kichik bo'lishi mumkin emas");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size 1 dan 100 gacha bo'lishi kerak");
        }

        String requestedSort = normalizeSort(sortBy, defaultSort);
        String sortProperty = allowedSorts.get(requestedSort);
        if (sortProperty == null) {
            throw new IllegalArgumentException("sortBy noto'g'ri: " + requestedSort);
        }

        Sort.Direction sortDirection = parseDirection(direction);
        Sort sort = Sort.by(sortDirection, sortProperty).and(Sort.by(Sort.Direction.ASC, "id"));
        return PageRequest.of(page, size, sort);
    }

    static String normalizeSort(String sortBy, String defaultSort) {
        if (sortBy == null || sortBy.isBlank()) {
            return defaultSort;
        }
        return sortBy.trim();
    }

    static String normalizeDirection(String direction) {
        return parseDirection(direction).name().toLowerCase();
    }

    private static Sort.Direction parseDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return Sort.Direction.ASC;
        }
        if ("asc".equalsIgnoreCase(direction.trim())) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(direction.trim())) {
            return Sort.Direction.DESC;
        }
        throw new IllegalArgumentException("direction faqat asc yoki desc bo'lishi kerak");
    }
}
