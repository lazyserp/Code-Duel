package com.codeduel.codeduel.ai.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeduel.codeduel.ai.dto.HintRequest;
import com.codeduel.codeduel.ai.dto.HintResponse;
import com.codeduel.codeduel.ai.service.AIHintService;
import com.codeduel.codeduel.auth.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class AIHintController 
{
    private final AIHintService aiService;

    @PostMapping("/{matchId}/hint")
    public ResponseEntity<HintResponse> getHint(@PathVariable UUID matchId, @AuthenticationPrincipal User user, @RequestBody HintRequest request) throws JsonProcessingException
    {
        HintResponse response = aiService.getOrCreateHint(matchId, user, request);
        return ResponseEntity.ok(response);
    }
    
}
