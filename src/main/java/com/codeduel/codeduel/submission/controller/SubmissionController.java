package com.codeduel.codeduel.submission.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeduel.codeduel.auth.model.User;
import com.codeduel.codeduel.submission.dto.SubmissionRequest;
import com.codeduel.codeduel.submission.model.Submission;
import com.codeduel.codeduel.submission.service.SubmissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController  {
    
    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<Submission> submit(@Valid @RequestBody SubmissionRequest request, @AuthenticationPrincipal User user)
    {
        Submission response = submissionService.createSubmission(request, user);
        return ResponseEntity.ok(response);


    }

    

    
}
