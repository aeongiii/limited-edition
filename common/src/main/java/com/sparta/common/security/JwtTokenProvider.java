package com.sparta.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {
    // Access 토큰 만료 시간 : 10분
    @Getter
    private final long accessTokenExpirationTime = 1000 * 60 * 10;
    // Refresh 토큰 만료 시간 : 7일
    @Getter
    private final long refreshTokenExpirationTime = 1000 * 60 * 60 * 24 * 7;

    @Value("${JWT_SECRET_KEY}")
    private String base64EncodedSecretKey;
    // 필요한 시점에 비밀키 생성
    private SecretKey getSecretKey() {
        if (base64EncodedSecretKey == null || base64EncodedSecretKey.isEmpty()) {
            System.out.println("JWT_SECRET_KEY가 null이거나 비어있습니다.");
            throw new IllegalArgumentException(".env 파일에서 JWT_SECRET_KEY 값을 읽을 수 없습니다.");
        }
        System.out.println("Base64 인코딩된 JWT_SECRET_KEY: " + base64EncodedSecretKey);

        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64EncodedSecretKey));
            System.out.println("비밀키 생성 성공");
            return key;
        } catch (Exception e) {
            System.out.println("비밀키 생성 중 오류 발생: " + e.getMessage());
            throw e;
        }
    }

    // 이메일 가지고 Access 토큰 생성
    public String generateAccessToken(String email) {
        System.out.println("Access토큰 생성 시작: 이메일 -> " + email);
        try {
            String token = Jwts.builder() // 이메일, 발급시간, 만료시간, 서명 알고리즘, 비밀키 지정
                    .setSubject(email) // 토큰의 subject에 이메일 저장
                    .setIssuedAt(new Date()) // 토큰 발급 시간
                    .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationTime)) // 만료 시간
                    .signWith(getSecretKey(), SignatureAlgorithm.HS512) // 비밀키, 서명알고리즘 설정
                    .compact(); // 문자열로 반환
            System.out.println("생성된 Access토큰: " + token);
            return token;
        } catch (Exception e) {
            System.out.println("Access토큰 생성 중 오류 발생: " + e.getMessage());
            throw e;
        }
    }

    // 이메일 가지고 Refresh 토큰 생성
    public String generateRefreshToken(String email) {
        System.out.println("Refresh토큰 생성 시작: 이메일 -> " + email);
        try {
            String token = Jwts.builder() // 이메일, 발급시간, 만료시간, 서명 알고리즘, 비밀키 지정
                    .setSubject(email) // 토큰의 subject에 이메일 저장
                    .setIssuedAt(new Date()) // 토큰 발급 시간
                    .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationTime)) // 만료 시간
                    .signWith(getSecretKey(), SignatureAlgorithm.HS512) // 비밀키, 서명알고리즘 설정
                    .compact(); // 문자열로 반환
            System.out.println("생성된 Refresh토큰: " + token);
            return token;
        } catch (Exception e) {
            System.out.println("Refresh토큰 생성 중 오류 발생: " + e.getMessage());
            throw e;
        }
    }

    // Access Token 검증 및 메시지 반환
    public void validateAccessToken(String accessToken) {
        if (accessToken == null || !validateToken(accessToken)) {
            throw new IllegalArgumentException("유효하지 않은 Access Token입니다.");
        }
        System.out.println("Access Token 검증 완료");
    }

    // Access Token 검증 및 이메일 반환
    public String validateAndExtractEmail(String accessToken) {
        if (accessToken == null || !validateToken(accessToken)) {
            throw new IllegalArgumentException("유효하지 않은 Access Token입니다.");
        }
        System.out.println("Access Token 검증 완료");
        return getEmailFromToken(accessToken);
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        System.out.println("jwt 토큰 검증 시작");
        try {
            // JWT 파싱, 검증
            Jwts.parserBuilder().setSigningKey(getSecretKey()).build().parseClaimsJws(token);
            System.out.println("jwt 토큰 검증 완료");
            return true;
        } catch (Exception e) {
            System.out.println("jwt 토큰 검증 실패");
            return false;
        }
    }

    // 토큰에서 email 추출
    public String getEmailFromToken(String token) {
        // claims 객체 가져오기 (JWT에서 사용자 정보, 만료시간 저장된 payload 부분)
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey()) // 비밀키 설정
                .build()
                .parseClaimsJws(token) // JWT 파싱
                .getBody();
        // claim에서 Subject 추출 -> email 반환
        return claims.getSubject();
    }
}
