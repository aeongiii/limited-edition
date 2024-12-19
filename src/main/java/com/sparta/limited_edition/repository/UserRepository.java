package com.sparta.limited_edition.repository;

import com.sparta.limited_edition.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일 중복확인
    boolean existsByEmail(String email);
}
