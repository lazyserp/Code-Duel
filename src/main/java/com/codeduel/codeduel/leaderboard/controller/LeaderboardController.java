package com.codeduel.codeduel.leaderboard.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.auth.repository.UserRepository;
import com.codeduel.codeduel.leaderboard.dto.LeaderBoardEntry;
import com.codeduel.codeduel.leaderboard.dto.LeaderboardResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    @GetMapping("")
    public LeaderboardResponse getLeaderboard(@AuthenticationPrincipal User currentUser) {
        // 1. Self-healing Sync if Redis is empty
        Long size = redisTemplate.opsForZSet().size("leaderboard:global");
        if (size == null || size == 0) {
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                redisTemplate.opsForZSet().add("leaderboard:global", u.getUsername(), u.getCurrentElo());
            }
        }

        //  Fetch Top 100 usernames from Redis
        Set<String> topUsernames = redisTemplate.opsForZSet().reverseRange("leaderboard:global", 0, 99);
        List<LeaderBoardEntry> topPlayers = new ArrayList<>();

        if (topUsernames != null && !topUsernames.isEmpty()) {
            // Load DB Profiles in bulk to avoid N+1 queries
            List<User> dbUsers = userRepository.findByUsernameIn(topUsernames);
            Map<String, User> userMap = dbUsers.stream()
                    .collect(Collectors.toMap(User::getUsername, Function.identity()));

            //  Build LeaderBoardEntry preserving Redis order
            int rank = 1;
            for (String username : topUsernames) {
                User u = userMap.get(username);
                if (u != null) {
                    topPlayers.add(mapToEntry(rank++, u));
                }
            }
        }

        //  Construct current user's ranking card
        LeaderBoardEntry currentUserRanking = null;
        if (currentUser != null) {
            User latestCurrentUser = userRepository.findByUsername(currentUser.getUsername())
                    .orElse(currentUser);
            Long myRankLong = redisTemplate.opsForZSet().reverseRank("leaderboard:global", latestCurrentUser.getUsername());
            int myRank = myRankLong != null ? myRankLong.intValue() + 1 : 0;
            currentUserRanking = mapToEntry(myRank, latestCurrentUser);
        }

        return new LeaderboardResponse(topPlayers, currentUserRanking);
    }

    private LeaderBoardEntry mapToEntry(int rank, User u) {
        int totalGames = u.getWins() + u.getLosses();
        int winRate = totalGames == 0 ? 0 : (u.getWins() * 100) / totalGames;
        
        String streakStr = "-";
        if (u.getStreak() > 0) {
            streakStr = "W" + u.getStreak();
        } else if (u.getStreak() < 0) {
            streakStr = "L" + Math.abs(u.getStreak());
        }

        return new LeaderBoardEntry(
            rank,
            u.getUsername(),
            u.getCurrentElo(),
            u.getWins(),
            u.getLosses(),
            winRate,
            streakStr,
            u.getTrend()
        );
    }
}
