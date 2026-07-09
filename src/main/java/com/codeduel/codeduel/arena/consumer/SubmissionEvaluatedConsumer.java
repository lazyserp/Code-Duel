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
import com.codeduel.codeduel.submission.model.Submission;
import com.codeduel.codeduel.submission.repository.SubmissionRepository;
import com.codeduel.codeduel.leaderboard.service.EloService;
import com.codeduel.codeduel.leaderboard.service.EloService.EloCalculationResult;
import java.util.List;

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
    private final SubmissionRepository submissionRepository;
    private final EloService eloService;

    @KafkaListener(topics = "submission-evaluated", groupId = "arena-evaluation-group")
    public void consume(SubmissionEvaluatedEvent event) {
        UUID matchId = event.matchId();
        log.info("Received submission-evaluated event for match: {}, status: {}", matchId, event.status());

        //  Broadcast the submission result to both players
        simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, event);

        // If status is ACCEPTED and it is a match submission, mark the match as finished
        if ("ACCEPTED".equalsIgnoreCase(event.status()) && event.isSubmit()) {
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

                    // Find the loser and calculate ELO changes
                    User loser = winner.getId().equals(match.getUser1().getId()) ? match.getUser2() : match.getUser1();
                    List<Submission> submissions = submissionRepository.findByMatchId(matchId);
                    int loserPassed = submissions.stream()
                        .filter(s -> s.getUser().getId().equals(loser.getId()))
                        .map(s -> s.getPassedCount() != null ? s.getPassedCount() : 0)
                        .max(Integer::compare)
                        .orElse(0);

                    EloCalculationResult eloResult = eloService.updateRatings(winner, loser, loserPassed, event.totalCount());

                    // Broadcast MATCH_ENDED event with ELO updates
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
            } catch (Exception e) {
                log.error("Failed to finalize match completion state for match: {}", matchId, e);
            }
        }
    }
}
