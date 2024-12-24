package com.sparta.limited_edition.service;

import com.sparta.limited_edition.dto.MyPageResponse;
import com.sparta.limited_edition.dto.RecentOrderResponse;
import com.sparta.limited_edition.dto.WishlistResponse;
import com.sparta.limited_edition.entity.User;
import com.sparta.limited_edition.exception.InvalidCredentialsException;
import com.sparta.limited_edition.repository.OrderRepository;
import com.sparta.limited_edition.repository.UserRepository;
import com.sparta.limited_edition.repository.WishlistRepository;
import com.sparta.limited_edition.security.EncryptionUtil;
import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.util.AuthNumberManager;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final MailService mailService;
    private final AuthenticationManager authenticationManager;
    private final AuthNumberManager authNumberManager;
    private final RedisTemplate<String, String> redisTemplate; // redis에 데이터 저장
    private final JwtTokenProvider jwtTokenProvider;
    private final WishlistRepository wishlistRepository;
    private final OrderRepository orderRepository;
    private final EncryptionUtil encryptionUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, MailService mailService, AuthenticationManager authenticationManager, AuthNumberManager authNumberManager, RedisTemplate<String, String> redisTemplate, JwtTokenProvider jwtTokenProvider, WishlistRepository wishlistRepository, OrderRepository orderRepository, EncryptionUtil encryptionUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.authenticationManager = authenticationManager;
        this.authNumberManager = authNumberManager;
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.wishlistRepository = wishlistRepository;
        this.orderRepository = orderRepository;
        this.encryptionUtil = encryptionUtil;
    }

    // 회원가입
    public User registerUser(String email, String password, String name, String address, String authNumber) {
        // 이메일 암호화
        String encryptedEmail;
        try {
            encryptedEmail = EncryptionUtil.encrypt(email);
        } catch (Exception e) {
            throw new RuntimeException("이메일 암호화 중 오류가 발생했습니다.", e);
        }
        // 이메일 중복체크
        if (userRepository.existsByEmail(encryptedEmail)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        System.out.println("이메일 중복체크 완료");

        // 인증번호 검증
        if (!authNumberManager.validateAuthNumber(email, authNumber)) {
            throw new IllegalArgumentException("인증번호를 다시 확인해주세요.");
        }
        System.out.println("이메일 인증 완료");

        // 비밀번호 암호화 (해싱)
        String encodedPassword = passwordEncoder.encode(password);
        // 개인정보 암호화 (AES)
        String encryptedName;
        String encryptedAddress;
        try {
            encryptedName = EncryptionUtil.encrypt(name);
            encryptedAddress = EncryptionUtil.encrypt(address);
        } catch (Exception e) {
            throw new RuntimeException("개인정보 암호화 중 오류가 발생했습니다.", e);
        }
        System.out.println("개인정보 암호화 완료");
        // 저장
        User user = new User();
        user.setEmail(encryptedEmail);
        user.setPassword(encodedPassword);
        user.setName(encryptedName);
        user.setAddress(encryptedAddress);
        userRepository.save(user);
        System.out.println("새로운 유저 DB에 저장 완료");
        return user;
    }

    // 로그인 요청 시 이메일, 비밀번호 인증
    public User authenticateUser(String email, String password) throws Exception {
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

    // 로그인 처리
    public Map<String, String> loginUser(String email, String password) throws Exception {
        // 사용자 이메일, 비밀번호 인증
        User user = authenticateUser(email, password);
        // A토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        // R토큰 생성
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // R토큰 -> Redis에 저장
        redisTemplate.opsForValue().set(
                "refresh:" + user.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpirationTime(), // 만료 시간 (밀리초 단위)
                TimeUnit.MILLISECONDS // 시간 단위
        );

        // A토큰과 R토큰을 Map으로 반환
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        return tokenMap;
    }

    // 마이페이지
    @Transactional(readOnly = true)
    public MyPageResponse getMypage(String email) throws Exception {
        // 유저 검증
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        // 개인정보 가져오기 (복호화)
        String decryptedEmail = EncryptionUtil.decrypt(user.getEmail());
        String decryptedName = EncryptionUtil.decrypt(user.getName());
        String decryptedAddress = EncryptionUtil.decrypt(user.getAddress());

        // 위시리스트 최신순 5개 가져오기
        List<WishlistResponse> wishlistResponseList = wishlistRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(wishlist -> new WishlistResponse(
                        wishlist.getProduct().getId(),
                        wishlist.getProduct().getName(),
                        wishlist.getQuantity(),
                        wishlist.getProduct().getPrice(),
                        wishlist.getProduct().getImageUrl(),
                        "http://localhost:8080/product/" + wishlist.getProduct().getId() // 상세정보 링크 생성
                )).toList();
        // 주문내역 최신순 5개 가져오기
        List<RecentOrderResponse> orderResponseList = orderRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(order -> new RecentOrderResponse(
                        order.getId(),
                        order.getStatus(),
                        order.getCreatedAt().toString()
                )).toList();

        // MypageResponse 생성
        return new MyPageResponse(
                decryptedEmail,
                decryptedName,
                decryptedAddress,
                user.getCreatedAt(),
                wishlistResponseList,
                orderResponseList
        );
    }
}
