package com.codeduel.codeduel.arena.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.codeduel.codeduel.arena.dto.MatchReadyResponse;
import com.codeduel.codeduel.arena.dto.ReadyRequest;
import com.codeduel.codeduel.arena.dto.TimerTickResponse;
import com.codeduel.codeduel.arena.dto.TypingRequest;
import com.codeduel.codeduel.arena.dto.TypingResponse;
import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.arena.repository.MatchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ArenaWebSocketController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final MatchRepository matchRepository;

    private final Map<UUID, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        
    @MessageMapping("/match/ready")
    public void matchReady(@Payload ReadyRequest request, Principal principal) {
        String username = principal.getName();
        UUID matchId = request.matchId();
        String redisKey = "match:" + matchId + ":ready";

        log.info("Player {} is ready for match {}", username, matchId);

        // Add player to the Redis Set
        stringRedisTemplate.opsForSet().add(redisKey, username);

        //  Check if both players are ready
        Long readyCount = stringRedisTemplate.opsForSet().size(redisKey);

        if (readyCount != null && readyCount == 2) {
            log.info("Both players ready for match {}. Starting match...", matchId);

            //  Fetch and update Match status in database
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
            
            if (!"ACTIVE".equals(match.getStatus())) {
                match.setStatus("ACTIVE");
                match.setStartedAt(LocalDateTime.now());
                matchRepository.save(match);
            }

            //  Broadcast MATCH_READY (ACTIVE status)
            simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, new MatchReadyResponse(matchId, "ACTIVE"));

            //  Clear the readiness set from Redis
            stringRedisTemplate.delete(redisKey);

            //  Start countdown timer (10 mins = 600s)
            startMatchTimer(matchId);
        }
    }

    private void startMatchTimer(UUID matchId) {
        // Cancel any existing timer for safety
        ScheduledFuture<?> existing = activeTimers.remove(matchId);
        if (existing != null) {
            existing.cancel(true);
        }

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(new Runnable() {
            int secondsRemaining = 600;

            @Override
            public void run() {
                secondsRemaining--;
                if (secondsRemaining <= 0) {
                    try {
                        Match match = matchRepository.findById(matchId).orElse(null);
                        if (match != null && !"FINISHED".equals(match.getStatus())) {
                            match.setStatus("FINISHED");
                            match.setEndedAt(LocalDateTime.now());
                            matchRepository.save(match);
                            log.info("Match {} finished by timeout", matchId);
                        }
                    } catch (Exception e) {
                        log.error("Failed to mark match as finished on timeout", e);
                    }

                    simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, new TimerTickResponse(matchId, "0"));
                    ScheduledFuture<?> self = activeTimers.remove(matchId);
                    if (self != null) {
                        self.cancel(false);
                    }
                } else {
                    simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, 
                        new TimerTickResponse(matchId, String.valueOf(secondsRemaining)));
                }
            }
        }, 1, 1, TimeUnit.SECONDS);

        activeTimers.put(matchId, task);
    }


    @MessageMapping("/match/typing")
    public void matchTyping(@Payload TypingRequest request,Principal principal)
    {
        String username = principal.getName();
        TypingResponse response = new TypingResponse(request.matchId(),username);

        simpMessagingTemplate.convertAndSend("/topic/match/" + request.matchId(),response);
    }
}
