package com.sparta.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String password;
    private String name;
    private String address;
    private LocalDateTime createdAt;

    public UserResponse(Long id, String email, String password, String name, String address, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.address = address;
        this.createdAt = createdAt;
    }

}
