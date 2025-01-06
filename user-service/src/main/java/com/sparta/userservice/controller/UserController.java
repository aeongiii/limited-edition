package com.sparta.userservice.controller;

import com.sparta.common.dto.MyPageResponse;
import com.sparta.common.security.JwtTokenProvider;
import com.sparta.common.util.AuthNumberManager;
import com.sparta.common.util.PasswordValidator;
import com.sparta.userservice.service.MailService;
import com.sparta.userservice.service.UserService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
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
        System.out.println("[UserController] email-auth 요청 수신");
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

//    // 로그아웃 -> A토큰 새로 만들때 필요하다면 jwtProvider 의존성 놔두고, 그렇지 않다면 삭제.
//    @PostMapping("/users/logout")
//    public ResponseEntity<String> logoutUser(@CookieValue(name = "accessToken", required = false) String accessToken) throws Exception {
//        String response = userService.logoutUser(accessToken); // A토큰 만료, R토큰 삭제 처리
//        return ResponseEntity.ok()
//                .header("Set-Cookie", response) // 만료된 A토큰 반환
//                .body("로그아웃 완료");
//    }

    // 마이페이지
    @GetMapping("users/mypage")
    public ResponseEntity<MyPageResponse> getMypage(@RequestHeader(name = "X-User-Email", required = true) String email) throws Exception {
        // 마이페이지 데이터 조회
        MyPageResponse response = userService.getMypage(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("users/test")
    public ResponseEntity<String> checkHeader(@RequestHeader(name = "X-User-Email", required = false) String email) {
        // 헤더 값 로그로 출력
        System.out.println("Received X-User-Email: " + email);

        // 헤더 값이 null인 경우
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("X-User-Email 헤더가 없습니다.");
        }

        // 헤더 값 반환
        return ResponseEntity.ok("Received X-User-Email: " + email);
    }

    @GetMapping("users/test2")
    public ResponseEntity<String> checkHeader(HttpServletRequest request) {
        String headerValue = request.getHeader("X-User-Email");
        System.out.println("X-User-Email: " + headerValue);
        return ResponseEntity.ok("X-User-Email: " + headerValue);
    }
}
