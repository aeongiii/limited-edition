package com.sparta.limited_edition.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRegisterRequest {
    private String email;
    private String password;
    private String name;
    private String address;
}
