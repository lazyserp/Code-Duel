package com.codeduel.codeduel.submission.dto;

import java.util.UUID;

public record SubmissionRequest(UUID matchId, String codeText, String language, Boolean isSubmit) {};