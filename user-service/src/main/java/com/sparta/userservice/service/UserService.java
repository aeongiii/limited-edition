package com.sparta.userservice.service;

import com.sparta.userservice.dto.MyPageResponse;
import com.sparta.userservice.dto.RecentOrderResponse;
import com.sparta.userservice.dto.WishlistResponse;
import com.sparta.userservice.entity.User;
import com.sparta.userservice.exception.InvalidCredentialsException;
import com.sparta.userservice.repository.OrderRepository;
import com.sparta.userservice.repository.UserRepository;
import com.sparta.userservice.repository.WishlistRepository;
import com.sparta.userservice.security.EncryptionUtil;
import com.sparta.userservice.security.JwtTokenProvider;
import com.sparta.userservice.util.AuthNumberManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// 유저 관련 서비스
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthNumberManager authNumberManager;
    private final RedisTemplate<String, String> redisTemplate; // redis에 데이터 저장
    private final JwtTokenProvider jwtTokenProvider;
    private final WishlistRepository wishlistRepository;
    private final OrderRepository orderRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, MailService mailService, AuthenticationManager authenticationManager, AuthNumberManager authNumberManager, RedisTemplate<String, String> redisTemplate, JwtTokenProvider jwtTokenProvider, WishlistRepository wishlistRepository, OrderRepository orderRepository, EncryptionUtil encryptionUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authNumberManager = authNumberManager;
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.wishlistRepository = wishlistRepository;
        this.orderRepository = orderRepository;
    }

    // 1. 회원가입
    public void registerUser(String email, String password, String name, String address, String authNumber) {
        String encryptedEmail = encryptEmail(email); // 이메일 암호화
        uniqueEmail(encryptedEmail); // 이메일 중복체크
        validateAuthNumber(email, authNumber); // 인증번호 검증
        String encodedPassword = passwordEncoder.encode(password); // 비밀번호 암호화 (해싱)
        // 개인정보 암호화
        String encryptedName = encryptData(name, "이름");
        String encryptedAddress = encryptData(address, "주소");
        // 저장
        saveUser(encryptedEmail, encodedPassword, encryptedName, encryptedAddress);
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

    // 3. 로그아웃
    public String logoutUser(String accessToken) throws Exception {

        jwtTokenProvider.validateAccessToken(accessToken); // Access Token 검증
        // Refresh 토큰 삭제
        String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        redisTemplate.delete("refresh:" + userId);
        // HTTP-only Cookie에 설정된 Access Token 만료시키기 (만료된 쿠키로 덮어씌움)
        ResponseCookie deleteAccessTokenCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true) // HTTP-only 쿠키로 설정
                .secure(false) // true : HTTPS를 통해서만 전송
                .path("/")  // 모든 경로에서 쿠ㅡ키 유효
                .maxAge(0) // 즉시 만료
                .build();
        System.out.println("로그아웃 완료");
        return deleteAccessTokenCookie.toString(); // 전송할 만료토큰 반환
    }


    // 4. 마이페이지
    @Transactional(readOnly = true)
    public MyPageResponse getMypage(String email) throws Exception {
        // 유저 검증
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        // 개인정보 가져오기 (복호화)
        String decryptedEmail = EncryptionUtil.decrypt(user.getEmail());
        String decryptedName = EncryptionUtil.decrypt(user.getName());
        String decryptedAddress = EncryptionUtil.decrypt(user.getAddress());
        // 위시리스트, 주문내역 최신순 5개 가져오기
        List<WishlistResponse> wishlistResponseList = getTop5Wishlists(user);
        List<RecentOrderResponse> orderResponseList = getTop5Orders(user);
        // MypageResponse 생성
        return new MyPageResponse(decryptedEmail, decryptedName, decryptedAddress,
                user.getCreatedAt(), wishlistResponseList, orderResponseList);
    }


    // =========================
    // 회원가입 - 이메일 암호화
    private String encryptEmail(String email) {
        try {
            return EncryptionUtil.encrypt(email);
        } catch (Exception e) {
            throw new RuntimeException("이메일 암호화 중 오류가 발생했습니다.", e);
        }
    }

    // 회원가입 - 이메일 중복 체크
    private void uniqueEmail(String encryptedEmail) {
        if (userRepository.existsByEmail(encryptedEmail)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        System.out.println("이메일 중복체크 완료");
    }

    // 회원가입 - 인증번호 검증
    private void validateAuthNumber(String email, String authNumber) {
        if (!authNumberManager.validateAuthNumber(email, authNumber)) {
            throw new IllegalArgumentException("인증번호를 다시 확인해주세요.");
        }
        System.out.println("이메일 인증 완료");
    }

    // 회원가입 - 데이터 암호화
    private String encryptData(String data, String fieldName) {
        try {
            return EncryptionUtil.encrypt(data);
        } catch (Exception e) {
            throw new RuntimeException(fieldName + " 암호화 중 오류가 발생했습니다.", e);
        }
    }

    // 회원가입 - 유저 엔티티 저장
    private User saveUser(String email, String password, String name, String address) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setName(name);
        user.setAddress(address);
        userRepository.save(user);
        System.out.println("새로운 유저 등록 완료");
        return user;
    }

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

    // 마이페이지 - 위시리스트 최신순 5개 가져오기
    private List<WishlistResponse> getTop5Wishlists(User user) {
        return wishlistRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(wishlist -> new WishlistResponse(
                        wishlist.getProduct().getId(),
                        wishlist.getProduct().getName(),
                        wishlist.getQuantity(),
                        wishlist.getProduct().getPrice(),
                        wishlist.getProduct().getImageUrl(),
                        "http://localhost:8080/product/" + wishlist.getProduct().getId() // 상세정보 링크 생성
                )).toList();
    }

    // 마이페이지 - 주문내역 최신순 5개 가져오기
    private List<RecentOrderResponse> getTop5Orders(User user) {
        return orderRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(order -> new RecentOrderResponse(
                        order.getId(),
                        order.getStatus(),
                        order.getCreatedAt().toString()
                )).toList();
    }
}
