package com.codeduel.codeduel.leaderboard.dto;


public record LeaderBoardEntry(
    int rank,
    String username,
    int elo,
    int wins,
    int losses,
    int winRate,
    String streak,
    String trend
){}