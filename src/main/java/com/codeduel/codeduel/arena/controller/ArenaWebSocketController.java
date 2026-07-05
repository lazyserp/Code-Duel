package com.codeduel.codeduel.arena.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.codeduel.codeduel.arena.dto.MatchReadyResponse;
import com.codeduel.codeduel.arena.dto.ReadyRequest;
import com.codeduel.codeduel.arena.dto.TypingRequest;
import com.codeduel.codeduel.arena.dto.TypingResponse;
import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.arena.repository.MatchRepository;
import com.codeduel.codeduel.arena.service.MatchManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ArenaWebSocketController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final MatchRepository matchRepository;
    private final MatchManager matchManager;

    @MessageMapping("/match/ready")
    public void matchReady(@Payload ReadyRequest request, Principal principal) {
        String username = principal.getName();
        UUID matchId = request.matchId();
        String redisKey = "match:" + matchId + ":ready";

        log.info("Player {} is ready for match {}", username, matchId);

        // 1Add player to the Redis Set
        stringRedisTemplate.opsForSet().add(redisKey, username);

        //  Check if both players are ready
        Long readyCount = stringRedisTemplate.opsForSet().size(redisKey);

        if (readyCount != null && readyCount == 2) {
            log.info("Both players ready for match {}. Starting match...", matchId);

            //  Fetch and update Match status in database
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
            if ( "FINISHED".equals(match.getStatus()))
            {
                return;
            }
            if (!"ACTIVE".equals(match.getStatus()) && !"FINISHED".equals(match.getStatus())) {
                match.setStatus("ACTIVE");
                match.setStartedAt(LocalDateTime.now());
                matchRepository.save(match);
            }

            // Broadcast MATCH_READY (ACTIVE status)
            simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, new MatchReadyResponse(matchId, "ACTIVE"));

            // Clear the readiness set from Redis
            stringRedisTemplate.delete(redisKey);

            //  Start countdown timer via the MatchManager
            matchManager.startTimer(matchId);
        }
    }

    @MessageMapping("/match/typing")
    public void matchTyping(@Payload TypingRequest request, Principal principal) {
        String username = principal.getName();
        TypingResponse response = new TypingResponse(request.matchId(), username);
        simpMessagingTemplate.convertAndSend("/topic/match/" + request.matchId(), response);
    }
}
