package com.maihehe.blogcore._06_DTO.request;


import lombok.Data;

@Data
public class LoginOrRegisterRequest {
    private String username;
    private String password;
}