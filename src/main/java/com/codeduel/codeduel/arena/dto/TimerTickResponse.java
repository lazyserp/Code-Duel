package com.codeduel.codeduel.arena.dto;

import java.util.UUID;

public record TimerTickResponse(UUID matchId, String secondsRemaining){};
