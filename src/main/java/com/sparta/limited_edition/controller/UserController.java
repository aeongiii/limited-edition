package com.sparta.limited_edition.controller;

import com.sparta.limited_edition.dto.JwtResponse;
import com.sparta.limited_edition.dto.UserRegisterRequest;
import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 회원가입
    @PostMapping("/users/signup")
    public ResponseEntity<JwtResponse> registerUser(@RequestBody UserRegisterRequest request) {
        userService.registerUser(request.getEmail(), request.getPassword(), request.getName(), request.getAddress());
        String token = jwtTokenProvider.generateToken(request.getEmail());
        return ResponseEntity.ok(new JwtResponse(token));
    }
}
