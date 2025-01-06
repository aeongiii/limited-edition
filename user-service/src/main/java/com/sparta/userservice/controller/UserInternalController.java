package com.sparta.userservice.controller;

import com.sparta.common.dto.UserResponse;
import com.sparta.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/user")
public class UserInternalController {

    private final UserService userService;

    public UserInternalController(UserService userService) {
        this.userService = userService;
    }

    // 이메일로 유저 찾기
    @GetMapping("/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}
