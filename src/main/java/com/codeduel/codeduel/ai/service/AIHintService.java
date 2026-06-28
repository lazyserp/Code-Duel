package com.codeduel.codeduel.ai.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.codeduel.codeduel.ai.dto.HintRequest;
import com.codeduel.codeduel.ai.dto.HintResponse;
import com.codeduel.codeduel.ai.model.AIHint;
import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.arena.model.Problem;
import com.codeduel.codeduel.arena.repository.MatchRepository;
import com.codeduel.codeduel.auth.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AIHintService {
    private final StringRedisTemplate redisTemplate;
    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;


    public HintResponse getOrCreateHint(UUID matchId, User user,HintRequest request) throws JsonProcessingException
    {
        String redisKey = "match:" + matchId + ":user:" + user.getId() + ":hint";
        String cachedValue = redisTemplate.opsForValue().get(redisKey);

        if ( cachedValue != null) 
        {
            AIHint aiHint = objectMapper.readValue(cachedValue, AIHint.class);
            
            return new HintResponse(aiHint.hint());
        }
        Match match = matchRepository.findById(matchId).orElseThrow(() -> new RuntimeException("Match Not Found !"));
        if ( !match.getStatus().equals("ACTIVE")) throw new RuntimeException("Message not Active");

        if (!match.getUser1().getId().equals(user.getId()) && !match.getUser2().getId().equals(user.getId()) ){ throw new RuntimeException("User is not a participant");};
        
        Problem problem = match.getProblem();

        String prompt = "You are a Socratic coding coach.\r\n" + //
                            "You are helping a candidate solve:" + problem.getTitle() +"\r\n" + //
                            "Problem Description:" + problem.getDescription() +"\r\n" + //
                            "\r\n" + //
                            "Here is the code they have written so far:\r\n" + //
                                request.codeText() +"\r\n" + //
                            "\r\n" + //
                            "Provide a Socratic hint that guides the user towards the next step without giving away the direct code solution.\r\n" + //
                            "";
        
        String LLMResponse = chatModel.call(prompt);

        AIHint aiHint = new AIHint(LLMResponse, prompt);
        redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(aiHint), Duration.ofMinutes(15));

        return new HintResponse(aiHint.hint());
    }   
}
