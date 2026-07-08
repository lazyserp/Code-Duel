package com.codeduel.codeduel.submission.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.codeduel.codeduel.submission.model.SubmissionStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CodeExecutorService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${executor.url:http://localhost:2358}")
    private String executorUrl;

    public ExecutionResult evaluate(String testCasesJson, String userCodeText, String language) {
        try {
            String base64Source = Base64.getEncoder().encodeToString(userCodeText.getBytes(StandardCharsets.UTF_8));

            log.info("Raw test cases JSON from DB: {}", testCasesJson);
            List<TestCase> testCases = objectMapper.readValue(testCasesJson, new TypeReference<List<TestCase>>() {});
            if (testCases == null || testCases.isEmpty()) {
                throw new IllegalStateException("Problem has no test cases loaded.");
            }
            log.info("Parsed test cases count: {}", testCases.size());

            List<ExecutorSubmissionRequest> submissions = testCases.stream().map(tc -> {
                String base64Input = Base64.getEncoder().encodeToString(tc.getInput().getBytes(StandardCharsets.UTF_8));
                String base64ExpectedOutput = Base64.getEncoder().encodeToString(tc.getOutput().getBytes(StandardCharsets.UTF_8));
                return new ExecutorSubmissionRequest(base64Source, language.toLowerCase(), base64Input, base64ExpectedOutput, 10, 256);
            }).collect(Collectors.toList());

            ExecutorBatchRequest requestBody = new ExecutorBatchRequest(submissions);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            String submitUrl = executorUrl + "/execute";
            log.info("Sending batch submissions to Custom Executor API at: {}", submitUrl);

            ResponseEntity<ExecutorBatchResponse> responseEntity = restTemplate.postForEntity(
                submitUrl,
                requestEntity,
                ExecutorBatchResponse.class
            );

            ExecutorBatchResponse responseBody = responseEntity.getBody();
            if (responseBody == null || responseBody.getResults() == null || responseBody.getResults().isEmpty()) {
                throw new RuntimeException("Failed to obtain execution results from Custom Executor.");
            }

            List<ExecutorExecutionResult> results = responseBody.getResults();
            SubmissionStatus finalStatus = SubmissionStatus.ACCEPTED;
            double maxTime = 0.0;
            int passedCount = 0;
            int totalCount = results.size();
            SubmissionStatus firstFailedStatus = null;

            for (ExecutorExecutionResult res : results) {
                double time = res.getTime() != null ? res.getTime() : 0.0;
                if (time > maxTime) {
                    maxTime = time;
                }

                if ("ACCEPTED".equalsIgnoreCase(res.getStatus())) {
                    passedCount++;
                } else if (firstFailedStatus == null) {
                    try {
                        firstFailedStatus = SubmissionStatus.valueOf(res.getStatus().toUpperCase());
                    } catch (Exception e) {
                        firstFailedStatus = SubmissionStatus.RUNTIME_ERROR;
                    }
                    log.warn("Test case failed execution. Status: {}, Time: {}s", res.getStatus(), time);
                }
            }

            SubmissionStatus statusToReturn = (firstFailedStatus != null) ? firstFailedStatus : SubmissionStatus.ACCEPTED;
            return new ExecutionResult(statusToReturn, (int) (maxTime * 1000), passedCount, totalCount);

        } catch (Exception e) {
            log.error("Execution error occurred during Custom Executor integration: ", e);
            return new ExecutionResult(SubmissionStatus.RUNTIME_ERROR, 0, 0, 0);
        }
    }

    // Inner DTO mappings
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCase {
        private String input;
        private String output;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExecutorSubmissionRequest {
        private String source_code;
        private String language;
        private String stdin;
        private String expected_output;
        private int time_limit;
        private int memory_limit;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExecutorBatchRequest {
        private List<ExecutorSubmissionRequest> submissions;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExecutorExecutionResult {
        private String status;
        private Double time;
        private int memory;
        private String stderr;
        private String compile_output;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExecutorBatchResponse {
        private List<ExecutorExecutionResult> results;
    }

    @Getter
    @AllArgsConstructor
    public static class ExecutionResult {
        private final SubmissionStatus status;
        private final int executionTimeMs;
        private final int passedCount;
        private final int totalCount;
    }
}
