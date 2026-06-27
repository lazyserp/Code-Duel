package com.codeduel.codeduel.arena.consumer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.codeduel.codeduel.arena.dto.MatchEndedResponse;
import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.arena.repository.MatchRepository;
import com.codeduel.codeduel.arena.service.MatchManager;
import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.auth.repository.UserRepository;
import com.codeduel.codeduel.submission.event.SubmissionEvaluatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionEvaluatedConsumer {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final MatchManager matchManager;

    @KafkaListener(topics = "submission-evaluated", groupId = "arena-evaluation-group")
    public void consume(SubmissionEvaluatedEvent event) {
        UUID matchId = event.matchId();
        log.info("Received submission-evaluated event for match: {}, status: {}", matchId, event.status());

        // 1. Broadcast the submission result to both players
        simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, event);

        // 2. If status is ACCEPTED, mark the match as finished
        if ("ACCEPTED".equalsIgnoreCase(event.status())) {
            try {
                Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

                if (!"FINISHED".equals(match.getStatus())) {
                    User winner = userRepository.findById(event.userId())
                        .orElseThrow(() -> new IllegalArgumentException("Winner user not found: " + event.userId()));

                    match.setStatus("FINISHED");
                    match.setWinner(winner);
                    match.setEndedAt(LocalDateTime.now());
                    matchRepository.save(match);

                    log.info("Match {} won by player: {}", matchId, winner.getUsername());

                    // Stop the active countdown timer
                    matchManager.stopTimer(matchId);

                    // Broadcast MATCH_ENDED event
                    simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, new MatchEndedResponse(matchId, event.userId()));
                }
            } catch (Exception e) {
                log.error("Failed to finalize match completion state for match: {}", matchId, e);
            }
        }
    }
}
