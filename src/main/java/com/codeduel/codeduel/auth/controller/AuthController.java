package com.codeduel.codeduel.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeduel.codeduel.auth.dto.AuthResponse;
import com.codeduel.codeduel.auth.dto.LoginRequest;
import com.codeduel.codeduel.auth.dto.RegisterRequest;
import com.codeduel.codeduel.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) 
    {
        AuthResponse res = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);

    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request)
    {
        AuthResponse res = authService.login(request);

       return  ResponseEntity.ok(res);

    }



    
}
