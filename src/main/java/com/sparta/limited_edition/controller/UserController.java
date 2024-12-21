package com.sparta.limited_edition.controller;

import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.service.MailService;
import com.sparta.limited_edition.service.UserService;
import com.sparta.limited_edition.util.AuthNumberManager;
import com.sparta.limited_edition.util.PasswordValidator;
import jakarta.mail.MessagingException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordValidator passwordValidator;
    private final MailService mailService;
    private final AuthNumberManager authNumberManager;
    private final RedisTemplate<String, String> redisTemplate;


    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider, PasswordValidator passwordValidator, MailService mailService, AuthNumberManager authNumberManager, RedisTemplate<String, String> redisTemplate) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordValidator = passwordValidator;
        this.mailService = mailService;
        this.authNumberManager = authNumberManager;
        this.redisTemplate = redisTemplate;
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
    public ResponseEntity<String> registerUser(@RequestBody Map<String, String> requestBody) {
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
        return ResponseEntity.ok("회원가입이 완료되었습니다.");    }

    // 로그인
    @PostMapping("/users/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody Map<String, String> requestBody) throws Exception {
        String email = requestBody.get("email");
        String password = requestBody.get("password");
        // 인증 후 A토큰, R토큰 생성
        Map<String, String> tokens = userService.loginUser(email, password);
        // Access token을 HTTP-Only Cookie에 저장
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", tokens.get("accessToken"))
                .httpOnly(true) // 자바스크립트 접근 불가 (보안 강화)
                .secure(false) // true : HTTPS를 통해서만 전송
                .path("/") // 모든 경로에서 쿠키 유효
                .maxAge(jwtTokenProvider.getAccessTokenExpirationTime() / 1000) // 만료 시간 10분
                .build();
        // R토큰은 JSON body 부분에 넣어져서 반환
        Map<String, String> response = new HashMap<>();
        response.put("refreshToken", tokens.get("refreshToken"));
        // 두 토큰 모두 반환
        return ResponseEntity.ok()
                .header("Set-Cookie", accessTokenCookie.toString()) // Access 토큰 쿠키에 넣어서 반환
                .body(response); // Refresh 토큰 JSON에 넣어서 반환
    }

    // 로그아웃
    @PostMapping("/users/logout")
    public ResponseEntity<String> logoutUser(@CookieValue(name = "accessToken", required = false) String accessToken) throws Exception {
        // Access 토큰 유효성 검사
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Token이 유효하지 않습니다.");
        }
        // Refresh 토큰에서 user_id 가져옴 -> redis에서 Refresh 토큰 삭제
        String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        redisTemplate.delete("refresh:" + userId);
        System.out.println("Refresh Token 삭제 완료");
        // HTTP-only Cookie에 설정된 Access Token 만료시키기 (만료된 쿠키로 덮어씌움)
        ResponseCookie deleteAccessTokenCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true) // HTTP-only 쿠키로 설정
                .secure(false) // true : HTTPS를 통해서만 전송
                .path("/")  // 모든 경로에서 쿠ㅡ키 유효
                .maxAge(0) // 즉시 만료
                .build();
        System.out.println("Access Token 만료");

        return ResponseEntity.ok()
                .header("Set-Cookie", deleteAccessTokenCookie.toString()) // 만료된 쿠키 반환
                .body("로그아웃 완료");
    }
}
