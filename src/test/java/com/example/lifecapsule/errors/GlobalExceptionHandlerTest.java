package com.example.lifecapsule.errors;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void forbiddenExceptionReturns403() {
        HttpServletRequest request = request("/families/1/persons");

        var response = handler.handleForbidden(new ForbiddenException("Ruxsat yo'q"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().path()).isEqualTo("/families/1/persons");
    }

    @Test
    void notFoundExceptionReturns404() {
        HttpServletRequest request = request("/families/999");

        var response = handler.handleNotFound(new NotFoundException("Topilmadi"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void conflictExceptionReturns409() {
        HttpServletRequest request = request("/auth/signup");

        var response = handler.handleConflict(new ConflictException("Allaqachon mavjud"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
    }

    @Test
    void unknownPathReturns404InsteadOf500() {
        HttpServletRequest request = request("/nope");

        var response = handler.handleNoResource(
                new org.springframework.web.servlet.resource.NoResourceFoundException(org.springframework.http.HttpMethod.GET, "nope"),
                request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    private HttpServletRequest request(String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }
}
