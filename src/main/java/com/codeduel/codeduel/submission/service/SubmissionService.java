package com.codeduel.codeduel.submission.service;

import java.util.Optional;
import java.util.UUID;

import javax.naming.NameNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import com.codeduel.codeduel.arena.model.Match;
import com.codeduel.codeduel.arena.repository.MatchRepository;
import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.submission.dto.SubmissionRequest;
import com.codeduel.codeduel.submission.event.SubmissionReceivedEvent;
import com.codeduel.codeduel.submission.model.Submission;
import com.codeduel.codeduel.submission.model.SubmissionStatus;
import com.codeduel.codeduel.submission.repository.SubmissionRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
    public class SubmissionService {
        private final SubmissionRepository repo;
        private final MatchRepository matchRepository;
        private final KafkaTemplate<String,SubmissionReceivedEvent> kafkaTemplate;


        public Submission createSubmission(SubmissionRequest request,User user)
        {
            
            Match match = matchRepository.findById(request.matchId()) .orElseThrow(() -> new RuntimeException("Match not found !"));
            {
                if ( match.getStatus() != "ACTIVE") throw new IllegalArgumentException();
            }

            if (!user.getId().equals(match.getUser1().getId()) && !user.getId().equals(match.getUser2().getId()))
            {
                throw new RuntimeException();
            }

            Submission submission = Submission.builder()
                                                .match(match)
                                                .user(user)
                                                .problem(match.getProblem())
                                                .codeText(request.codeText())
                                                .language(request.language())
                                                .status(SubmissionStatus.PENDING)
                                                .build();
            repo.save(submission);
            SubmissionReceivedEvent response = new SubmissionReceivedEvent(submission.getId(),submission.getMatch().getId(),submission.getUser().getId(),submission.getCodeText(),submission.getLanguage());
                                                                        
            kafkaTemplate.send("submission-received",response);

            return submission;
                                                    
        }

        
    }
