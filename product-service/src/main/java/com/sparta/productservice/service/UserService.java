package com.sparta.productservice.service;

import com.sparta.productservice.entity.User;
import com.sparta.productservice.exception.InvalidCredentialsException;
import com.sparta.productservice.repository.UserRepository;
import com.sparta.productservice.security.EncryptionUtil;
import com.sparta.productservice.security.JwtTokenProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// 유저 관련 서비스
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate; // redis에 데이터 저장
    private final JwtTokenProvider jwtTokenProvider;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, RedisTemplate<String, String> redisTemplate, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;

    }


    // 2. 로그인
    public ResponseEntity<Map<String, String>> loginUser(String email, String password) throws Exception {
        // A토큰, R토큰 생성
        Map<String, String> tokens = createAccessTokenAndRefreshToken(email, password);
        // Access Token을 HTTP-Only Cookie로 생성
        ResponseCookie accessTokenCookie = createAccessTokenCookie(tokens.get("accessToken"));
        // Refresh Token Json으로 반환
        Map<String, String> response = Map.of("refreshToken", tokens.get("refreshToken"));
        return ResponseEntity.ok()
                .header("Set-Cookie", accessTokenCookie.toString()) // 헤더에 쿠키 포함
                .body(response); // 바디에 R토큰 포함
    }





    // =========================




    // 로그인 - A토큰, R토큰 생성
    private Map<String, String> createAccessTokenAndRefreshToken(String email, String password) throws Exception {
        User user = authenticateUser(email, password); // 사용자 이메일, 비밀번호 인증
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail()); // A토큰 생성
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail()); // R토큰 생성
        saveRefreshTokenToRedis(refreshToken, user); // R토큰 Redis에 저장

        // A토큰과 R토큰을 Map으로 반환
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        return tokenMap;
    }

    // 로그인 - 이메일, 비밀번호 인증
    private User authenticateUser(String email, String password) throws Exception {
        // 유저 존재하는지 확인
        User user = userRepository.findByEmail(EncryptionUtil.encrypt(email))
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 이메일 복호화 + 비밀번호 일치하는지 확인
        try {
            String decryptedEmail = EncryptionUtil.decrypt(user.getEmail()); // 이메일 복호화
            System.out.println("복호화된 이메일: " + decryptedEmail);
            System.out.println("입력받은 이메일: " + email);
            // 이메일이 다르면 예외 발생
            if (!decryptedEmail.equals(email)) {
                throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }
            // 비밀번호 다르면 예외 발생
            if (!passwordEncoder.matches(password, user.getPassword())) {
                System.out.println("입력받은 비밀번호: " + password);
                System.out.println("암호화된 비밀번호: " + user.getPassword());
                throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }
            System.out.println("로그인 성공");

        } catch (InvalidCredentialsException e) {
            System.out.println("인증 오류: " + e.getMessage());
            throw e; // 클라이언트로 반환
        } catch (Exception e) {
            System.out.println("예상치 못한 오류: " + e.getMessage());
            throw new RuntimeException("로그인 중 오류가 발생했습니다.", e);
        }
        return user;
    }

    // 로그인 - A토큰을 HTTP-only Cookie로 생성
    private ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true) // 자바스크립트 접근 불가 (보안 강화)
                .secure(false) // true : HTTPS를 통해서만 전송
                .path("/") // 모든 경로에서 쿠키 유효
                .maxAge(jwtTokenProvider.getAccessTokenExpirationTime() / 1000) // 만료 시간
                .build();
    }

    // 로그인 - R토큰 Redis에 저장
    private void saveRefreshTokenToRedis(String refreshToken, User user) {
        // R토큰 -> Redis에 저장
        redisTemplate.opsForValue().set(
                "refresh:" + user.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpirationTime(), // 만료 시간 (밀리초 단위)
                TimeUnit.MILLISECONDS // 시간 단위
        );
        System.out.println("R토큰 Redis에 저장 완료");
    }


}
