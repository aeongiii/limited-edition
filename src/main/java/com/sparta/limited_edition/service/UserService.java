package com.sparta.limited_edition.service;

import com.sparta.limited_edition.entity.User;
import com.sparta.limited_edition.repository.UserRepository;
import com.sparta.limited_edition.security.EncryptionUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// 유저 관련 서비스
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입
    public User registerUser(String email, String password, String name, String address) {
        // 이메일 중복체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        System.out.println("이메일 중복체크 완료");

        // 비밀번호는 복호화할 수 없는 해싱 사용
        String encodedPassword = passwordEncoder.encode(password);

        // 나머지 개인정보는 복호화 가능하게 암호화해서 저장
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
