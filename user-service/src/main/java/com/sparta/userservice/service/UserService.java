package com.sparta.userservice.service;

import com.sparta.common.dto.MyPageResponse;
import com.sparta.common.dto.RecentOrderResponse;
import com.sparta.common.dto.UserResponse;
import com.sparta.common.dto.WishlistResponse;
import com.sparta.common.exception.DuplicateEmailException;
import com.sparta.common.exception.InvalidCredentialsException;
import com.sparta.common.security.EncryptionUtil;
import com.sparta.common.security.JwtTokenProvider;
import com.sparta.common.util.AuthNumberManager;
import com.sparta.userservice.client.OrderServiceClient;
import com.sparta.userservice.client.ProductServiceClient;
import com.sparta.userservice.client.WishlistServiceClient;
import com.sparta.userservice.entity.User;
import com.sparta.userservice.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
    private final ProductServiceClient productServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final WishlistServiceClient wishlistServiceClient;
    private final EncryptionUtil encryptionUtil;


    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthNumberManager authNumberManager,
                       RedisTemplate<String, String> redisTemplate,
                       JwtTokenProvider jwtTokenProvider,
                       EncryptionUtil encryptionUtil,
                       ProductServiceClient productServiceClient,
                       OrderServiceClient orderServiceClient,
                       WishlistServiceClient wishlistServiceClient, EncryptionUtil encryptionUtil1) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authNumberManager = authNumberManager;
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.productServiceClient = productServiceClient;
        this.orderServiceClient = orderServiceClient;
        this.wishlistServiceClient = wishlistServiceClient;
        this.encryptionUtil = encryptionUtil1;
    }

    // 1. 회원가입
    public void registerUser(String email, String password, String name, String address, String authNumber) {
        String encryptedEmail = encryptEmail(email);
        uniqueEmail(encryptedEmail);
        validateAuthNumber(email, authNumber);
        String encodedPassword = passwordEncoder.encode(password);
        String encryptedName = encryptData(name, "이름");
        String encryptedAddress = encryptData(address, "주소");
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
                .header("Set-Cookie", accessTokenCookie.toString())
                .body(response);
    }

    // 3. 로그아웃
    public String logoutUser(String accessToken) throws Exception {
        jwtTokenProvider.validateAccessToken(accessToken); // Access Token 검증
        // Refresh 토큰 삭제
        String userId = jwtTokenProvider.getEmailFromToken(accessToken);
        redisTemplate.delete("refresh:" + userId);
        // HTTP-only Cookie에 설정된 Access Token 만료시키기 (만료된 쿠키로 덮어씌움)
        ResponseCookie deleteAccessTokenCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true) // HTTP-only 쿠키로 설정
                .secure(false) // true : HTTPS를 통해서만 전송
                .path("/")  // 모든 경로에서 쿠키 유효
                .maxAge(0) // 즉시 만료
                .build();
        System.out.println("로그아웃 완료");
        return deleteAccessTokenCookie.toString(); // 전송할 만료토큰 반환
    }


    // 4. 마이페이지
    @Transactional(readOnly = true)
    public MyPageResponse getMypage(String email) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("회원 정보를 찾을 수 없습니다."));
        String decryptedEmail = encryptionUtil.encrypt(user.getEmail());
        String decryptedName = encryptionUtil.encrypt(user.getName());
        String decryptedAddress = encryptionUtil.decrypt(user.getAddress());
        List<WishlistResponse> wishlistResponseList = getTop5Wishlists(user);
        List<RecentOrderResponse> orderResponseList = getTop5Orders(user);
        return new MyPageResponse(decryptedEmail, decryptedName, decryptedAddress,
                user.getCreatedAt(), wishlistResponseList, orderResponseList);
    }

    // user 객체 보내기
    public UserResponse getUserByEmail(String email) {
        try {
        String encryptedEmail = encryptionUtil.encrypt(email);
        User user = userRepository.findByEmail(encryptedEmail)
                .orElse(null);
        if (user == null) {
            throw new InvalidCredentialsException("사용자를 찾을 수 없습니다.");
        }
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getName(),
                user.getAddress(),
                user.getCreatedAt()
        );
        } catch (Exception e) {
            // 예외 처리
            throw new RuntimeException("사용자 조회 중 오류가 발생했습니다.", e);
        }
    }


    // =========================
    // 회원가입 - 이메일 암호화
    private String encryptEmail(String email) {
        try {
            return encryptionUtil.encrypt(email);
        } catch (Exception e) {
            throw new RuntimeException("이메일 암호화 중 오류가 발생했습니다.", e);
        }
    }

    // 회원가입 - 이메일 중복 체크
    private void uniqueEmail(String encryptedEmail) {
        if (userRepository.existsByEmail(encryptedEmail)) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }
        System.out.println("이메일 중복체크 완료");
    }

    // 회원가입 - 인증번호 검증
    private void validateAuthNumber(String email, String authNumber) {
        if (!authNumberManager.validateAuthNumber(email, authNumber)) {
            throw new InvalidCredentialsException("인증번호를 다시 확인해주세요.");
        }
        System.out.println("이메일 인증 완료");
    }

    // 회원가입 - 데이터 암호화
    private String encryptData(String data, String fieldName) {
        try {
            return encryptionUtil.encrypt(data);
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
        User user = authenticateUser(email, password);
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
        saveRefreshTokenToRedis(refreshToken, user);

        // A토큰과 R토큰을 Map으로 반환
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        return tokenMap;
    }

    // 로그인 - 이메일, 비밀번호 인증
    private User authenticateUser(String email, String password) throws Exception {
        // 유저 존재하는지 확인
        User user = userRepository.findByEmail(encryptionUtil.encrypt(email))
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 이메일 복호화 + 비밀번호 일치하는지 확인
        try {
            String decryptedEmail = encryptionUtil.decrypt(user.getEmail());
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
            throw e;
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
        return wishlistServiceClient.getTop5Wishlist(user.getId());
    }

    // 마이페이지 - 주문내역 최신순 5개 가져오기
    private List<RecentOrderResponse> getTop5Orders(User user) {
        return orderServiceClient.getTop5OrderList(user.getId());

    }
}
