package com.codeduel.codeduel.submission.event;

import java.util.UUID;

import lombok.Builder;

@Builder
public record SubmissionReceivedEvent(
    UUID submissionId,
    UUID matchId,
    UUID userId,
    String code,
    String language
) {};
