package com.example.lifecapsule.web.rest;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.service.UserService;
import com.example.lifecapsule.service.dto.UserSearchResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserSearchResource {
    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResultDto>> search(
            @AuthenticationPrincipal Users currentUser,
            @RequestParam(name = "query", defaultValue = "") String query
    ) {
        List<UserSearchResultDto> result = userService.searchByUsername(currentUser, query);
        return ResponseEntity.ok().body(result);
    }
}
