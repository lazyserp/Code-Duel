package com.codeduel.codeduel.ai.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.codeduel.codeduel.ai.dto.HintRequest;
import com.codeduel.codeduel.ai.dto.HintResponse;
import com.codeduel.codeduel.ai.model.AIHint;
import com.codeduel.codeduel.ai.repository.AIHintRepository;
import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.arena.model.Problem;
import com.codeduel.codeduel.arena.repository.MatchRepository;
import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.submission.model.Submission;
import com.codeduel.codeduel.submission.repository.SubmissionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIHintService {

    private final StringRedisTemplate redisTemplate;
    private final MatchRepository matchRepository;
    private final SubmissionRepository submissionRepository;
    private final AIHintRepository aiHintRepository;
    private final ChatModel chatModel;

    public HintResponse getOrCreateHint(UUID matchId, User user, HintRequest request) {
        String redisKey = "match:" + matchId + ":user:" + user.getId() + ":hint";
        String cachedHintText = redisTemplate.opsForValue().get(redisKey);

        // 1. Check Redis Cache
        if (cachedHintText != null) {
            log.info("Serving cached hint from Redis for user: {} in match: {}", user.getUsername(), matchId);
            return new HintResponse(cachedHintText);
        }

        // 2. Validate Match & User
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        if (!"ACTIVE".equals(match.getStatus())) {
            throw new IllegalStateException("Match is not active: " + matchId);
        }

        if (!match.getUser1().getId().equals(user.getId()) && !match.getUser2().getId().equals(user.getId())) {
            throw new SecurityException("User is not a participant in this match.");
        }

        // 3. Resolve the latest user submission for linking
        List<Submission> submissions = submissionRepository.findByMatchId(matchId);
        UUID latestSubmissionId = submissions.stream()
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .max((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()))
                .map(Submission::getId)
                .orElse(null);

        Problem problem = match.getProblem();

        // 4. Construct Socratic Prompt
        String prompt = "You are a Socratic coding coach.\r\n" +
                "You are helping a candidate solve: " + problem.getTitle() + "\r\n" +
                "Problem Description: " + problem.getDescription() + "\r\n\r\n" +
                "Here is the code they have written so far:\r\n" +
                request.codeText() + "\r\n\r\n" +
                "Provide a Socratic hint that guides the user towards the next step without giving away the direct code solution.\r\n";

        log.info("Requesting Socratic hint from LLM for user: {}", user.getUsername());

        // 5. Call LLM (NVIDIA NIM) capturing ChatResponse
        ChatResponse chatResponse = chatModel.call(new Prompt(prompt));
        String llmResponse = chatResponse.getResult().getOutput().getText();

        // Extract usage metadata from response
        Usage usage = chatResponse.getMetadata().getUsage();
        int totalTokens = (usage != null && usage.getTotalTokens() != null) ? usage.getTotalTokens() : 0;
        log.info("Token usage for hint: total={}, prompt={}, completion={}", 
                totalTokens, 
                usage != null ? usage.getPromptTokens() : 0, 
                usage != null ? usage.getCompletionTokens() : 0);

        // 6. Save to PostgreSQL database with token metadata
        AIHint aiHint = new AIHint(llmResponse, prompt);
        aiHint.setSubmissionId(latestSubmissionId);
        aiHint.setModelUsed("meta/llama-3.1-8b-instruct");
        aiHint.setTokensConsumed(totalTokens);
        aiHint = aiHintRepository.save(aiHint);

        log.info("Persisted AI Hint to database with ID: {}", aiHint.getId());

        // 7. Write raw string to Redis Cache (15 min TTL)
        redisTemplate.opsForValue().set(redisKey, llmResponse, Duration.ofMinutes(15));

        return new HintResponse(llmResponse);
    }
}
