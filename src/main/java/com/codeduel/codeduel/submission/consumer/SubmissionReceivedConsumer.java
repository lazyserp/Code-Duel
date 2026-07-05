package com.codeduel.codeduel.submission.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.codeduel.codeduel.submission.event.SubmissionEvaluatedEvent;
import com.codeduel.codeduel.submission.event.SubmissionReceivedEvent;
import com.codeduel.codeduel.submission.model.Submission;
import com.codeduel.codeduel.submission.repository.SubmissionRepository;
import com.codeduel.codeduel.submission.service.CodeExecutorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionReceivedConsumer {

    private final CodeExecutorService codeExecutorService;
    private final SubmissionRepository submissionRepository;
    private final KafkaTemplate<String, SubmissionEvaluatedEvent> kafkaTemplate;

    @KafkaListener(topics = "submission-received", groupId = "submission-execution-group")
    public void consume(SubmissionReceivedEvent event) {
        log.info("Received submission event for evaluation: {}", event.submissionId());
        
        try {
            //  Fetch submission record from database
            Submission submission = submissionRepository.findById(event.submissionId())
                .orElseThrow(() -> new IllegalArgumentException("Submission not found in database: " + event.submissionId()));

            // Call Code Executor evaluation engine
            CodeExecutorService.ExecutionResult result = codeExecutorService.evaluate(
                submission.getProblem().getTitle(),
                submission.getProblem().getTestCases(),
                event.code(),
                submission.getLanguage()
            );

            // Update database state
            submission.setStatus(result.getStatus());
            submission.setExecutionTime(result.getExecutionTimeMs());
            submissionRepository.save(submission);

            log.info("Submission {} evaluated with status: {} (time: {}ms)", 
                submission.getId(), result.getStatus(), result.getExecutionTimeMs());

            //  Publish conclusion event to submission-evaluated topic
            SubmissionEvaluatedEvent evaluatedEvent = SubmissionEvaluatedEvent.builder()
                .submissionId(submission.getId())
                .matchId(submission.getMatch().getId())
                .userId(submission.getUser().getId())
                .status(result.getStatus().name())
                .executionTime(result.getExecutionTimeMs())
                .build();

            kafkaTemplate.send("submission-evaluated", evaluatedEvent);
            log.info("Dispatched SubmissionEvaluatedEvent to Kafka for submission: {}", submission.getId());

        } catch (Exception e) {
            log.error("Failed to execute code execution for submission: {}", event.submissionId(), e);
        }
    }
}



// [Client / Postman]
//        |  (1) POST /api/submissions (JSON payload)
//        v
// [SubmissionController]
//        |  (2) Delegates to
//        v
// [SubmissionService] 
//        |  (3) Validates Match & Participant
//        |  (4) Persists PENDING Submission in Postgres
//        |  (5) Publishes SubmissionReceivedEvent to Kafka
//        v
//   [Kafka: "submission-received" Topic]
//        |
//        |  (6) Picked up asynchronously by
//        v
// [SubmissionReceivedConsumer]
//        |  (7) Fetches Problem & Test Cases from Postgres
//        |  (8) Passes code to
//        v
// [Judge0service]
//        |  (9) Concatenates User Code with Driver Wrapper (Main class)
//        |  (10) Submits batch test cases to Judge0 CE (Docker Container)
//        |  (11) Polls Judge0 /submissions/batch until complete
//        |  (12) Aggregates outcomes and maximum execution time
//        v
// [SubmissionReceivedConsumer] (Resumes)
//        |  (13) Saves final status (e.g. ACCEPTED/WRONG_ANSWER) in Postgres
//        |  (14) Publishes SubmissionEvaluatedEvent to Kafka
//        v
//   [Kafka: "submission-evaluated" Topic] --> (Prepares for Week 3 WebSockets)