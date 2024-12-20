package com.sparta.limited_edition.controller;

import com.sparta.limited_edition.dto.JwtResponse;
import com.sparta.limited_edition.dto.UserRegisterRequest;
import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.service.MailService;
import com.sparta.limited_edition.service.UserService;
import com.sparta.limited_edition.util.AuthNumberManager;
import com.sparta.limited_edition.util.PasswordValidator;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordValidator passwordValidator;
    private final MailService mailService;
    private final AuthNumberManager authNumberManager;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider, PasswordValidator passwordValidator, MailService mailService, AuthNumberManager authNumberManager) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordValidator = passwordValidator;
        this.mailService = mailService;
        this.authNumberManager = authNumberManager;
    }

    // 회원가입 전 이메일 중복확인 + 이메일 인증번호 발송
    @PostMapping ("/users/email-auth")
    public ResponseEntity<String> sendAuthNumber(@RequestBody Map<String, String> requestBody) throws MessagingException {
        String email = requestBody.get("email");
        String authNumber = mailService.sendMail(email); // 인증번호 생성 및 이메일 발송
        authNumberManager.saveAuthNumber(email, authNumber, 300); // 인증번호 저장 (유효시간 5분)
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    // 회원가입 (이메일 인증번호 함께 전달)
    @PostMapping("/users/signup")
    public ResponseEntity<JwtResponse> registerUser(@RequestBody Map<String, String> requestBody) {
        // 요청에서 필요한 값 추출
        String email = requestBody.get("email");
        String password = requestBody.get("password");
        String name = requestBody.get("name");
        String address = requestBody.get("address");
        String authNumber = requestBody.get("authNumber"); // 이메일 인증번호
        // 비밀번호 유효성 검증
        if (!PasswordValidator.isValidPassword(password)) {
            return ResponseEntity.badRequest().body(new JwtResponse("비밀번호는 최소 8자리 이상, 영문, 숫자, 특수문자를 포함해야 합니다."));
        }
        // 회원가입 시작
        userService.registerUser(email, password, name, address, authNumber);
        String token = jwtTokenProvider.generateToken(email);
        return ResponseEntity.ok(new JwtResponse(token));
    }
}
