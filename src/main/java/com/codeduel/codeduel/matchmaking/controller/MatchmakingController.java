package com.codeduel.codeduel.matchmaking.controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.matchmaking.dto.MatchmakingRequest;

import com.codeduel.codeduel.matchmaking.service.MatchmakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/matchmaking")
@RequiredArgsConstructor
public class MatchmakingController 
{
    private final MatchmakingService matchmakingService;

    @PostMapping("/join")
    public ResponseEntity<?> joinQueue(@Valid @RequestBody MatchmakingRequest request)
    {
        Optional<Match> match = matchmakingService.joinQueue(request.userId(),request.difficulty());

        if ( match.isEmpty())
        {
            return ResponseEntity.ok(Map.of(
                "status","QUEUED",
                "message","Waiting in queue for an opponent..."
            ));
        }

        return ResponseEntity.ok(match.get());        
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveMatch(@RequestParam UUID userId)
    {
        return ResponseEntity.ok(matchmakingService.getActiveMatch(userId));

    }
    
}
