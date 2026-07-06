package com.codeduel.codeduel.user.controller;



import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.auth.repository.UserRepository;
import com.codeduel.codeduel.user.dto.UserProfileResponse;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/users")
@RequiredArgsConstructor
@RestController
public class UserController {
    private final UserRepository userRepository;

    @GetMapping("/me")
    public UserProfileResponse profileController(@AuthenticationPrincipal User user)
    {
        User profileUser = userRepository.findById(user.getId()).orElseThrow( () -> new IllegalArgumentException("No Users Found"));
        return (new UserProfileResponse(profileUser.getId(),profileUser.getUsername(),profileUser.getEmail(),profileUser.getCurrentElo()));

    }
    
}
