package com.codeduel.codeduel.submission.service;

import org.springframework.kafka.core.KafkaTemplate;

import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.arena.repository.MatchRepository;
import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.submission.dto.SubmissionRequest;
import com.codeduel.codeduel.submission.event.SubmissionReceivedEvent;
import com.codeduel.codeduel.submission.model.Submission;
import com.codeduel.codeduel.submission.model.SubmissionStatus;
import com.codeduel.codeduel.submission.repository.SubmissionRepository;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
    public class SubmissionService {
        private final SubmissionRepository repo;
        private final MatchRepository matchRepository;
        private final KafkaTemplate<String,SubmissionReceivedEvent> kafkaTemplate;


        public Submission createSubmission(SubmissionRequest request,User user)
        {
            
            Match match = matchRepository.findById(request.matchId()) .orElseThrow(() -> new RuntimeException("Match not found !"));
            {
                if ( !match.getStatus().equals("ACTIVE")) throw new IllegalArgumentException();
            }

            if (!user.getId().equals(match.getUser1().getId()) && !user.getId().equals(match.getUser2().getId()))
            {
                throw new RuntimeException("User is not a participant in this match");
            }

            Submission submission = Submission.builder()
                                                .match(match)
                                                .user(user)
                                                .problem(match.getProblem())
                                                .codeText(request.codeText())
                                                .language(request.language())
                                                .status(SubmissionStatus.PENDING)
                                                .isSubmit(request.isSubmit() != null ? request.isSubmit() : true)
                                                .build();
            repo.save(submission);
            SubmissionReceivedEvent response = new SubmissionReceivedEvent(
                submission.getId(),
                submission.getMatch().getId(),
                submission.getUser().getId(),
                submission.getCodeText(),
                submission.getLanguage(),
                submission.isSubmit()
            );
                                                                        
            kafkaTemplate.send("submission-received",response);

            return submission;
                                                    
        }
    }


// ┌─────────────────────────────────────────────────────────────────┐
// │                    CLIENT (Browser/Postman)                     │
// └───────────────────────────┬─────────────────────────────────────┘
//                             │
//                 POST /api/submissions
//                 Headers: Authorization: Bearer <JWT>
//                 Body: {matchId, codeText, language}
//                             │
//                             ▼
// ┌─────────────────────────────────────────────────────────────────┐
// │               Spring Security Filter Chain                       │
// │  1. Extract JWT from Authorization header                       │
// │  2. Validate token (signature, expiration)                      │
// │  3. Load User entity from database                              │
// │  4. Inject into @AuthenticationPrincipal                        │
// └───────────────────────────┬─────────────────────────────────────┘
//                             │
//                             ▼
// ┌─────────────────────────────────────────────────────────────────┐
// │                  SubmissionController                            │
// │  submit(SubmissionRequest request, User user)                   │
// │  - Receives deserialized request + authenticated user           │
// │  - Delegates to service layer                                   │
// └───────────────────────────┬─────────────────────────────────────┘
//                             │
//                             ▼
// ┌─────────────────────────────────────────────────────────────────┐
// │                   SubmissionService                              │
// │                                                                  │
// │  Step 1: Validate Match Exists                                  │
// │    matchRepository.findById(matchId)                            │
// │    ↓ Match found                                               │
// │                                                                  │
// │  Step 2: Validate Match is Active                               │
// │    if (status != "ACTIVE") throw error                          │
// │    ↓ Status = "ACTIVE"                                         │
// │                                                                  │
// │  Step 3: Validate User is Participant                           │
// │    if (user != user1 && user != user2) throw error              │
// │    ↓ User is player1                                           │
// │                                                                  │
// │  Step 4: Create Submission Entity                               │
// │    Submission.builder()...                                      │
// │    ↓ Built                                                     │
// │                                                                  │
// │  Step 5: Save to Database                                       │
// │    repo.save(submission)                                        │
// │    ↓ Saved to PostgreSQL                                       │
// │                                                                  │
// │  Step 6: Publish Event to Kafka                                 │
// │    kafkaTemplate.send("submission-received", event)             │
// │    ↓ Published                                                 │
// │                                                                  │
// │  Step 7: Return Submission                                      │
// │    return submission                                            │
// └───────────────────────────┬─────────────────────────────────────┘
//                             │
//             ┌───────────────┴────────────────┐
//             │                                │
//             ▼                                ▼
// ┌────────────────────────┐      ┌────────────────────────┐
// │   Return to Client     │      │   Kafka Topic          │
// │   HTTP 200 OK          │      │   "submission-received"│
// │   Body: Submission     │      │   Event stored        │
// │   {                    │      └───────────┬────────────┘
// │     id: "sub-123",     │                  │
// │     status: "PENDING"  │                  │ (Async)
// │   }                    │                  ▼
// └────────────────────────┘      ┌────────────────────────┐
//                                 │SubmissionReceivedConsumer│
//                                 │  (Next step!)           │
//                                 └────────────────────────┘

