package com.codeduel.codeduel.leaderboard.dto;

import java.util.List;

public record LeaderboardResponse(     
    List<LeaderBoardEntry> topPlayers,
    LeaderBoardEntry currentUserRanking
) {}
