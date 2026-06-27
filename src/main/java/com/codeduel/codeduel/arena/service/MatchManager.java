package com.codeduel.codeduel.arena.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.codeduel.codeduel.arena.dto.TimerTickResponse;
import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.arena.repository.MatchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchManager {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MatchRepository matchRepository;

    private final Map<UUID, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public void startTimer(UUID matchId) {
        // Cancel any existing timer for safety
        stopTimer(matchId);

        log.info("Starting gameplay countdown timer for match: {}", matchId);

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(new Runnable() {
            int secondsRemaining = 600; // 10 minutes

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
                            log.info("Match {} ended by timeout limit.", matchId);
                        }
                    } catch (Exception e) {
                        log.error("Failed to mark match as finished on timeout", e);
                    }

                    simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, new TimerTickResponse(matchId, "0"));
                    stopTimer(matchId);
                } else {
                    simpMessagingTemplate.convertAndSend("/topic/match/" + matchId, 
                        new TimerTickResponse(matchId, String.valueOf(secondsRemaining)));
                }
            }
        }, 1, 1, TimeUnit.SECONDS);

        activeTimers.put(matchId, task);
    }

    public void stopTimer(UUID matchId) {
        ScheduledFuture<?> task = activeTimers.remove(matchId);
        if (task != null) {
            task.cancel(true);
            log.info("Stopped timer task for match: {}", matchId);
        }
    }
}
