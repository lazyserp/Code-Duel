package com.codeduel.codeduel.matchmaking.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.codeduel.codeduel.arena.model.Difficulty;
import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.arena.model.Problem;
import com.codeduel.codeduel.arena.repository.MatchRepository;
import com.codeduel.codeduel.arena.repository.ProblemRepository;
import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.auth.repository.UserRepository;
import com.codeduel.codeduel.matchmaking.event.MatchCreatedEvent;
import lombok.RequiredArgsConstructor;


import org.springframework.kafka.core.KafkaTemplate;

@Service  
@RequiredArgsConstructor  
public class MatchmakingService {

    private final KafkaTemplate<String, MatchCreatedEvent> kafkaTemplate;

    // Repository to fetch and persist user data from PostgreSQL
    private final UserRepository userRepository;
    
    // Repository to fetch coding problems by difficulty from PostgreSQL
    private final ProblemRepository problemRepository;
    
    // Repository to save created match records to PostgreSQL
    private final MatchRepository matchRepository;
    
    // Redis template for interacting with Redis Sorted Sets (matchmaking queue)
    private final StringRedisTemplate redisTemplate;


    public Optional<Match> joinQueue(UUID userId, Difficulty difficulty) {
        // Fetch user from database to get their current ELO rating
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Extract ELO rating (used as score in Redis Sorted Set)
        int userElo = user.getCurrentElo();
        
        // Construct Redis key for difficulty-specific queue (e.g., "matchmaking:queue:MEDIUM")
        String queueKey = "matchmaking:queue:" + difficulty.name();
        
        //  Convert UUID to String for Redis storage
        String userIdStr = userId.toString();

        //  Atomically try to find and claim an opponent from the queue
        // This prevents race conditions where multiple users try to match with the same opponent
        String opponentIdStr = findAndRemoveOpponent(queueKey, userIdStr, userElo);

        //  If no opponent found, add this user to the queue and return empty
        if (opponentIdStr == null) {
            // Add user to Redis Sorted Set with their ELO as the score
            // This allows efficient range queries to find opponents within ±100 ELO
            redisTemplate.opsForZSet().add(queueKey, userIdStr, userElo);
            return Optional.empty(); // User is now waiting in queue
        }

        //  Opponent found! Fetch opponent's user record from database
        User opponent = userRepository.findById(UUID.fromString(opponentIdStr))
                .orElseThrow(() -> new RuntimeException("Opponent not found"));

        // Fetch all problems matching the requested difficulty level
        List<Problem> problems = problemRepository.findByDifficulty(difficulty);
        
        // Validate that problems exist for this difficulty (prevent empty list error)
        if (problems.isEmpty()) {
            throw new RuntimeException("No problems available for difficulty: " + difficulty);
        }
        
        //  Randomly select one problem from the available pool
        Problem selectedProblem = problems.get(new Random().nextInt(problems.size()));

        //  Build the Match entity with both users, selected problem, and metadata
        Match match = Match.builder()
                .user1(user)                      // First player
                .user2(opponent)                  // Matched opponent
                .problem(selectedProblem)         // Coding challenge for this duel
                .status("ACTIVE")                 // Match is now live
                .startedAt(LocalDateTime.now())   // Timestamp for timer/analytics
                .build();

        //  Persist match to database and send an event in Kafka and then return wrapped in Optional
        Match matchDetails = matchRepository.save(match);
        MatchCreatedEvent matchEvent = new MatchCreatedEvent(
                                                            matchDetails.getId(),
                                                            matchDetails.getUser1().getId(),
                                                            matchDetails.getUser2().getId(),
                                                            matchDetails.getProblem().getId()
                                                            );

        kafkaTemplate.send("match-created",matchEvent);

        return Optional.of(matchDetails);
    }

    /**
     * Searches for an opponent within ELO range and atomically removes them from the queue.
     * This method prevents race conditions by using Redis's atomic remove operation.
     * If multiple threads try to claim the same opponent, only one will succeed.
     */

    private String findAndRemoveOpponent(String queueKey, String userId, int userElo) {
        //  Query Redis Sorted Set for all users in ELO range [userElo-100, userElo+100]
        // This ensures balanced matches by only pairing similarly-skilled players
        Set<String> candidates = redisTemplate.opsForZSet().rangeByScore(
                queueKey, 
                userElo - 100,  // Lower bound of acceptable ELO
                userElo + 100   // Upper bound of acceptable ELO
        );

        //  If no candidates found in range, return null (user will be added to queue)
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        //  Iterate through candidates and try to atomically claim one
        for (String candidateId : candidates) {
            // Skip if the candidate is the current user (shouldn't match with themselves)
            if (candidateId.equals(userId)) {
                continue;
            }

            // Atomically remove candidate from Redis queue
            // Redis ZREM is atomic: returns 1 if element removed, 0 if already gone
            // This acts as a distributed lock - only one thread can successfully remove
            Long removed = redisTemplate.opsForZSet().remove(queueKey, candidateId);
            
            // If removal succeeded, this thread claimed the opponent
            if (removed != null && removed > 0) {
                return candidateId; // Return opponent ID to create match
            }
            
            // If removed == 0, another thread already matched with this opponent
            // Loop continues to try the next candidate
        }

        // All candidates were already claimed by other threads (race lost)
        // Return null so current user gets added to queue instead
        return null;
    }
}
