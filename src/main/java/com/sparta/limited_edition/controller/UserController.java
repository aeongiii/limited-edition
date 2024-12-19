package com.sparta.limited_edition.controller;

import com.sparta.limited_edition.dto.JwtResponse;
import com.sparta.limited_edition.dto.UserRegisterRequest;
import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.service.UserService;
import com.sparta.limited_edition.util.PasswordValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordValidator passwordValidator;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider, PasswordValidator passwordValidator) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordValidator = passwordValidator;
    }

    // 회원가입
    @PostMapping("/users/signup")
    public ResponseEntity<JwtResponse> registerUser(@RequestBody UserRegisterRequest request) {
        String password = request.getPassword();
        // 비밀번호 유효성 검증
        if (!PasswordValidator.isValidPassword(password)) {
            return ResponseEntity.badRequest().body(new JwtResponse("비밀번호는 최소 8자리 이상, 영문, 숫자, 특수문자를 포함해야 합니다."));
        }

        userService.registerUser(request.getEmail(), request.getPassword(), request.getName(), request.getAddress());
        String token = jwtTokenProvider.generateToken(request.getEmail());
        return ResponseEntity.ok(new JwtResponse(token));
    }
}
