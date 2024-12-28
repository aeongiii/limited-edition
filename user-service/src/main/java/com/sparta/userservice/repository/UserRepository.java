package com.sparta.userservice.repository;

import com.sparta.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일 중복확인
    boolean existsByEmail(String email);

    // 로그인 시 유저 있는지 확인
    Optional<User> findByEmail(String email);
}
