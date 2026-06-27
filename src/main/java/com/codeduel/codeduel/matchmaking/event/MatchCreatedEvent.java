package com.codeduel.codeduel.matchmaking.event;

import java.util.UUID;

public record MatchCreatedEvent(UUID matchId, UUID user1Id, UUID user2Id, UUID problemId){};