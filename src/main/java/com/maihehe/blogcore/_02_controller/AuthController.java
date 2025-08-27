package com.maihehe.blogcore._02_controller;

import com.maihehe.blogcore._03_service.Impl.AuthService;
import com.maihehe.blogcore._06_DTO.request.LoginOrRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "AuthController 用户认证")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "用户注册",description = "注册成功后返回 JWT")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody LoginOrRegisterRequest request) {
        try {
            String token = authService.register(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 登陆，生成 JWT 并返回
    @Operation(summary = "用户登陆")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginOrRegisterRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(Map.of("token", token));
    }
}
