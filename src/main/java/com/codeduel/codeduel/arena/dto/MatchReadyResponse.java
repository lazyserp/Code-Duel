package com.codeduel.codeduel.arena.dto;

import java.util.UUID;

public record MatchReadyResponse(UUID matchId, String status){};
