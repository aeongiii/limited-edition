package com.sparta.limited_edition.service;

import com.sparta.limited_edition.entity.User;
import com.sparta.limited_edition.exception.InvalidCredentialsException;
import com.sparta.limited_edition.repository.UserRepository;
import com.sparta.limited_edition.security.EncryptionUtil;
import com.sparta.limited_edition.util.AuthNumberManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// 유저 관련 서비스
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuthenticationManager authenticationManager;
    private final AuthNumberManager authNumberManager;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, MailService mailService, AuthenticationManager authenticationManager, AuthNumberManager authNumberManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.authenticationManager = authenticationManager;
        this.authNumberManager = authNumberManager;
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
}
