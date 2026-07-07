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
import com.codeduel.codeduel.arena.dto.MatchEndedResponse;
import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.arena.repository.MatchRepository;
import com.codeduel.codeduel.arena.service.MatchManager;
import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.auth.repository.UserRepository;
import com.codeduel.codeduel.leaderboard.service.EloService;
import com.codeduel.codeduel.leaderboard.service.EloService.EloCalculationResult;

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
    private final UserRepository userRepository;
    private final EloService eloService;

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

    @MessageMapping("/match/forfeit")
    public void matchForfeit(@Payload ReadyRequest request, Principal principal) {
        String username = principal.getName();
        UUID matchId = request.matchId();
        log.info("Player {} requested forfeit for match {}", username, matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        if (!"ACTIVE".equals(match.getStatus())) {
            log.warn("Cannot forfeit match {} in status {}", matchId, match.getStatus());
            return;
        }

        User loser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        
        User winner = match.getUser1().getId().equals(loser.getId()) ? match.getUser2() : match.getUser1();

        match.setStatus("FINISHED");
        match.setWinner(winner);
        match.setEndedAt(LocalDateTime.now());
        matchRepository.save(match);

        matchManager.stopTimer(matchId);

        EloCalculationResult eloResult = eloService.updateRatings(winner, loser, 0, 0);

        simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, new MatchEndedResponse(
                matchId,
                winner.getId(),
                eloResult.winnerEloBefore(),
                eloResult.winnerEloAfter(),
                eloResult.winnerChange(),
                eloResult.loserEloBefore(),
                eloResult.loserEloAfter(),
                eloResult.loserChange()
        ));
    }
}
