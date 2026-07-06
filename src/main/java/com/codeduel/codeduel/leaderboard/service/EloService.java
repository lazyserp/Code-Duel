package com.codeduel.codeduel.leaderboard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EloService {

    private final UserRepository userRepository;
    private static final int K_FACTOR = 32;

    public record EloCalculationResult(
        int winnerEloBefore,
        int winnerEloAfter,
        int winnerChange,
        int loserEloBefore,
        int loserEloAfter,
        int loserChange
    ) {}


    @Transactional
    public EloCalculationResult updateRatings(User winner, User loser, int loserPassed, int totalTests) {
        int winnerElo = winner.getCurrentElo();
        int loserElo = loser.getCurrentElo();

        //. Calculate Expected Scores
        double expectedWinner = 1.0 / (1.0 + Math.pow(10.0, (loserElo - winnerElo) / 400.0));
        double expectedLoser = 1.0 / (1.0 + Math.pow(10.0, (winnerElo - loserElo) / 400.0));

        //  Calculate Standard Rating Changes
        int winnerChange = (int) Math.round(K_FACTOR * (1.0 - expectedWinner));
        int standardLoserChange = (int) Math.round(K_FACTOR * (0.0 - expectedLoser)); // Negative value

        //  Mitigate Loser's Loss based on test case performance ratio
        double mitigationRatio = 0.0;
        if (totalTests > 0) {
            mitigationRatio = (double) Math.min(loserPassed, totalTests) / totalTests;
        }
        
        // If mitigationRatio is 1.0 (passed all tests), loserChange becomes 0
        int loserChange = (int) Math.round(standardLoserChange * (1.0 - mitigationRatio));

        // Ensure ELO does not drop below a reasonable floor (e.g., 100 ELO)
        int newWinnerElo = winnerElo + winnerChange;
        int newLoserElo = Math.max(100, loserElo + loserChange);
        
        // Re-calculate actual change after applying floor constraints
        int actualLoserChange = newLoserElo - loserElo;

        log.info("ELO Update: Winner {} ({} -> {} [+{}]), Loser {} ({} -> {} [{}]) | Mitigated by: {}%",
            winner.getUsername(), winnerElo, newWinnerElo, winnerChange,
            loser.getUsername(), loserElo, newLoserElo, actualLoserChange, Math.round(mitigationRatio * 100));

        //  Persist to Database
        winner.setCurrentElo(newWinnerElo);
        loser.setCurrentElo(newLoserElo);
        userRepository.save(winner);
        userRepository.save(loser);

        return new EloCalculationResult(
            winnerElo, newWinnerElo, winnerChange,
            loserElo, newLoserElo, actualLoserChange
        );
    }
}
