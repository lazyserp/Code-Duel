package com.codeduel.codeduel.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.codeduel.codeduel.auth.dto.AuthResponse;
import com.codeduel.codeduel.auth.dto.LoginRequest;
import com.codeduel.codeduel.auth.dto.RegisterRequest;
import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.auth.repository.UserRepository;
import com.codeduel.codeduel.common.util.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService 
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;

    public AuthResponse register(RegisterRequest request)
    {
        if (userRepository.existsByUsername(request.username()) )
        {
            throw new IllegalArgumentException("Username Already Exists");
        }

        if (userRepository.existsByEmail(request.email()) )
        {
            throw new IllegalArgumentException("Email Already Exists");
        }

        var hashedPassword = passwordEncoder.encode(request.password());
        User user = User.builder()
                                .username(request.username())
                                .email(request.email())
                                .passwordHash(hashedPassword)
                                .build();
        userRepository.save(user);
        redisTemplate.opsForZSet().add("leaderboard:global",user.getUsername(),user.getCurrentElo());

        
        var token = jwtService.generateToken(request.username());

        return new AuthResponse(token,request.username(),request.email(),user.getId().toString());
                    
    }

    public AuthResponse login(LoginRequest request)
    {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(request.username(),request.password());
        authenticationManager.authenticate(token);

        User user = userRepository.findByUsername(request.username()).orElseThrow();

        String loginToken = jwtService.generateToken(user.getUsername());

        return new AuthResponse(loginToken,request.username(),user.getEmail(),user.getId().toString());


        
    }
    
}
