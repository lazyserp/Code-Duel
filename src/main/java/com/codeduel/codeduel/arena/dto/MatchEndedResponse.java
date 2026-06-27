package com.codeduel.codeduel.arena.dto;

import java.util.UUID;

public record MatchEndedResponse(UUID matchId, UUID winnerId) {}
