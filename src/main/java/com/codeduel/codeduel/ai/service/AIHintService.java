package com.codeduel.codeduel.ai.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.ai.openai.chat.options.model}")
    private String modelName ;

    @Value("${spring.ai.openai.api-key}")
    private String apiKeyProp;

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
        String prompt = """
                            You are CodeDuel's AI Hint Coach.

                            The user is solving:

                            Title: %s

                            Problem Description:
                            %s

                            Current Code:
                            %s

                            Your task is to provide ONLY the next incremental hint.

                            STRICT RULES:
                            - Reveal exactly ONE new insight.
                            - Assume the user can ask for another hint later.
                            - Never jump ahead or explain the full solution.
                            - Maximum 2 short sentences.
                            - Maximum 50 words.
                            - No code.
                            - No pseudocode.
                            - No implementation steps.
                            - No full algorithm explanation.
                            - No time or space complexity.
                            - No examples unless absolutely necessary.
                            - Do not praise or criticize the user.
                            - Do not repeat the problem statement.
                            - Do not say "Let's think about this", "Consider this", or similar filler.
                            - Avoid sounding like a teacher giving a lecture.
                            - Sound like an interviewer giving a subtle nudge.

                            Prioritize hints in this order:
                            1. Point the user's attention to the missing observation.
                            2. If they're still far away, hint at the property the solution needs.
                            3. Only if they're close, hint toward the appropriate data structure or technique.
                            4. Never reveal the complete approach in a single hint.

                            Examples of GOOD hints:
                            - "The order of unmatched brackets is important."
                            - "Think about what information needs to persist while scanning left to right."
                            - "When you encounter a closing bracket, what earlier information must still be available?"
                            - "Ask yourself whether every opening bracket can be matched immediately."

                            Examples of BAD hints:
                            - "Use a stack because it follows LIFO..."
                            - "Maintain a stack, push opening brackets, pop when..."
                            - Long explanations.
                            - Multiple questions.
                            - Step-by-step algorithms.
                            - Any code or pseudocode.

                            Return ONLY the hint text. No markdown. No bullets. No quotes.
                            """
                            .formatted(
                                problem.getTitle(),
                                problem.getDescription(),
                                request.codeText()
                            );

        log.info("Requesting Socratic hint from LLM for user: {}", user.getUsername());

        String llmResponse;
        int totalTokens = 0;
        try {
            if (apiKeyProp == null || "mock-key".equalsIgnoreCase(apiKeyProp) || apiKeyProp.trim().isEmpty()) {
                throw new IllegalStateException("spring.ai.openai.api-key property is not configured");
            }

            ChatResponse chatResponse = chatModel.call(new Prompt(prompt));
            llmResponse = chatResponse.getResult().getOutput().getText();
            Usage usage = chatResponse.getMetadata().getUsage();
            totalTokens = (usage != null && usage.getTotalTokens() != null) ? usage.getTotalTokens() : 0;

            log.info("Token usage for hint: total={}, prompt={}, completion={}", 
                    totalTokens, 
                    usage != null ? usage.getPromptTokens() : 0, 
                    usage != null ? usage.getCompletionTokens() : 0);
        } catch (Exception e) {
            log.warn("Failed to retrieve live AI hint: {}. Using offline Socratic fallback helper.", e.getMessage());
            llmResponse = "Offline Coach Hint: Analyze the problem constraints for '" + problem.getTitle() + 
                          "'. Verify your loop bounds, watch out for boundary check violations, " +
                          "and check if recursion base cases are terminating properly.";
            totalTokens = 30;
        }

        // 6. Save to PostgreSQL database with token metadata
        AIHint aiHint = new AIHint(llmResponse, prompt);
        aiHint.setSubmissionId(latestSubmissionId);
        aiHint.setModelUsed(modelName);
        aiHint.setTokensConsumed(totalTokens);
        aiHint = aiHintRepository.save(aiHint);

        log.info("Persisted AI Hint to database with ID: {}", aiHint.getId());

        // 7. Write raw string to Redis Cache (15 min TTL)
        redisTemplate.opsForValue().set(redisKey, llmResponse, Duration.ofMinutes(15));

        return new HintResponse(llmResponse);
    }
}
