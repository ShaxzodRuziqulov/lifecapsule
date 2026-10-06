package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.repository.UserRepository;
import com.example.lifecapsule.service.AuthenticationService;
import com.example.lifecapsule.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthenticationResource.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationResourceTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void signupReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "wrong-email",
                                  "firstName": "",
                                  "lastName": "",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Kiritilgan ma'lumotlarda xatolik bor"))
                .andExpect(jsonPath("$.path").value("/auth/signup"))
                .andExpect(jsonPath("$.fields.username").exists())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.firstName").exists())
                .andExpect(jsonPath("$.fields.lastName").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() throws Exception {
        Users user = user();
        when(authenticationService.authenticate(any())).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.getExpirationTime()).thenReturn(1000L);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "lifecapsule_user",
                                  "password": "Secure123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1000));
    }

    @Test
    void refreshReturnsNewAccessAndRefreshTokens() throws Exception {
        Users user = user();
        when(jwtService.extractUserId("old-refresh-token")).thenReturn(1L);
        when(authenticationService.findById(1L)).thenReturn(user);
        when(jwtService.isRefreshTokenValid("old-refresh-token", user)).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");
        when(jwtService.getExpirationTime()).thenReturn(1000L);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "old-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1000));
    }

    private Users user() {
        Users user = new Users();
        user.setId(1L);
        user.setEmail("user@lifecapsule.uz");
        user.setUserName("lifecapsule_user");
        return user;
    }
}
