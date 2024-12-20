package com.sparta.limited_edition.security;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {
    // Access 토큰 만료 시간 : 10분
    private final long accessTokenExpirationTime = 1000 * 60 * 10;
    // Refresh 토큰 만료 시간 : 7일
    @Getter
    private final long refreshTokenExpirationTime = 1000 * 60 * 60 * 24 * 7;

    // Base64 인코딩된 비밀키를 .env에서 읽어온다.
    private final Dotenv dotenv = Dotenv.configure().load(); // .env 파일 가져오기
    private final String base64EncodedSecretKey = dotenv.get("JWT_SECRET_KEY"); // 키 가져오기

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

    // access 토큰 검증 + 토큰 유효성 확인 -> 이메일 반환
    public String validateTokenAndGetSubject(String token) {
        System.out.println("토큰 검증 시작: 토큰 -> " + token);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey()) // 서명 검증을 위해 비밀키 설정
                    .build()
                    .parseClaimsJws(token) // jwt 파싱
                    .getBody(); // Claims 객체(jwt 데이터) 반환
            System.out.println("토큰 검증 성공: subject -> " + claims.getSubject());
            return claims.getSubject(); // Claims 객체에서 이메일 반환
        } catch (Exception e) {
            System.out.println("토큰 검증 중 오류 발생: " + e.getMessage());
            throw e;
        }
    }

}