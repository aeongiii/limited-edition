package com.sparta.userservice.controller;

import com.sparta.userservice.security.JwtTokenProvider;
import com.sparta.userservice.service.UserService;
import com.sparta.userservice.util.AuthNumberManager;
import com.sparta.userservice.util.PasswordValidator;
import com.sparta.userservice.dto.MyPageResponse;
import com.sparta.userservice.service.MailService;
import jakarta.mail.MessagingException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final AuthNumberManager authNumberManager;


    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider, PasswordValidator passwordValidator, MailService mailService, AuthNumberManager authNumberManager, RedisTemplate<String, String> redisTemplate) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.mailService = mailService;
        this.authNumberManager = authNumberManager;
    }

    // 회원가입 전 이메일 중복확인 + 이메일 인증번호 발송
    @PostMapping("/users/email-auth")
    public ResponseEntity<String> sendAuthNumber(@RequestBody Map<String, String> requestBody) throws MessagingException {
        String email = requestBody.get("email");
        String authNumber = mailService.sendMail(email); // 인증번호 생성 및 이메일 발송
        authNumberManager.saveAuthNumber(email, authNumber, 300); // 인증번호 저장 (유효시간 5분)
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    // 회원가입 (이메일 인증번호 함께 전달)
    @PostMapping("/users/signup")
    public ResponseEntity<String> signUpUser(@RequestBody Map<String, String> requestBody) {
        // 요청에서 필요한 값 추출
        String email = requestBody.get("email");
        String password = requestBody.get("password");
        String name = requestBody.get("name");
        String address = requestBody.get("address");
        String authNumber = requestBody.get("authNumber"); // 이메일 인증번호
        // 비밀번호 유효성 검증
        if (!PasswordValidator.isValidPassword(password)) {
            return ResponseEntity.badRequest().body("비밀번호는 최소 8자리 이상, 영문, 숫자, 특수문자를 포함해야 합니다.");
        }
        // 회원가입 시작
        userService.registerUser(email, password, name, address, authNumber);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    // 로그인
    @PostMapping("/users/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody Map<String, String> requestBody) throws Exception {
        return userService.loginUser(requestBody.get("email"), requestBody.get("password"));
    }

    // 로그아웃
    @PostMapping("/users/logout")
    public ResponseEntity<String> logoutUser(@CookieValue(name = "accessToken", required = false) String accessToken) throws Exception {
        String response = userService.logoutUser(accessToken); // A토큰 만료, R토큰 삭제 처리
        return ResponseEntity.ok()
                .header("Set-Cookie", response) // 만료된 A토큰 반환
                .body("로그아웃 완료");
    }

    // 마이페이지
    @GetMapping("users/mypage")
    public ResponseEntity<MyPageResponse> getMypage(@CookieValue(name = "accessToken", required = false) String accessToken) throws Exception {
        // Access Token 검증
        jwtTokenProvider.validateAccessToken(accessToken);
        // 이메일 추출
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        // 마이페이지 데이터 조회
        MyPageResponse response = userService.getMypage(email);
        return ResponseEntity.ok(response);
    }
}
