package com.codeduel.codeduel.matchmaking.dto;

import java.util.UUID;
import com.codeduel.codeduel.arena.model.Difficulty;
import jakarta.validation.constraints.NotNull;

public record MatchmakingRequest(
    @NotNull(message="User Id cannot be NULL") UUID userId,
    @NotNull(message = "Difficulty cannot be NULL") Difficulty difficulty
){};

