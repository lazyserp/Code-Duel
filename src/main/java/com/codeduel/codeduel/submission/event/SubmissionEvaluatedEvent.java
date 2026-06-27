package com.codeduel.codeduel.submission.event;

import java.util.UUID;

import lombok.Builder;

@Builder
public record SubmissionEvaluatedEvent(
    UUID submissionId,
    UUID matchId,
    UUID userId,
    String status,
    int executionTime
){};
