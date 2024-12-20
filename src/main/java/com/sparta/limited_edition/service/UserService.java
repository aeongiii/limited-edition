package com.sparta.limited_edition.service;

import com.sparta.limited_edition.entity.User;
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
        String encryptedEmail;
        try {
            encryptedName = EncryptionUtil.encrypt(name);
            encryptedAddress = EncryptionUtil.encrypt(address);
            encryptedEmail = EncryptionUtil.encrypt(email);
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

}
