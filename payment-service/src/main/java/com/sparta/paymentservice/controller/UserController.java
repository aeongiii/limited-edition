package com.sparta.paymentservice.controller;

import com.sparta.paymentservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {
    private final UserService userService;



    public UserController(UserService userService) {
        this.userService = userService;

    }


    // 로그인
    @PostMapping("/users/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody Map<String, String> requestBody) throws Exception {
        return userService.loginUser(requestBody.get("email"), requestBody.get("password"));
    }


}
