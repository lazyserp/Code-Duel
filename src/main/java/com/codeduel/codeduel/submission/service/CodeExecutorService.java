package com.codeduel.codeduel.submission.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // Static mapping of problem titles to their execution runner wrappers
    private static final Map<String, String> RUNNER_WRAPPERS = new HashMap<>();

    static {
        RUNNER_WRAPPERS.put("Contains Duplicate",
            "import java.io.*;\n" +
            "import java.util.*;\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) throws Exception {\n" +
            "        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n" +
            "        String line = br.readLine();\n" +
            "        if (line == null) return;\n" +
            "        line = line.trim();\n" +
            "        if (line.startsWith(\"[\")) line = line.substring(1);\n" +
            "        if (line.endsWith(\"]\")) line = line.substring(0, line.length() - 1);\n" +
            "        if (line.isEmpty()) {\n" +
            "            System.out.print(new Solution().containsDuplicate(new int[0]));\n" +
            "            return;\n" +
            "        }\n" +
            "        int[] nums = Arrays.stream(line.split(\",\"))\n" +
            "                           .map(String::trim)\n" +
            "                           .mapToInt(Integer::parseInt)\n" +
            "                           .toArray();\n" +
            "        System.out.print(new Solution().containsDuplicate(nums));\n" +
            "    }\n" +
            "}\n"
        );

        RUNNER_WRAPPERS.put("Valid Anagram",
            "import java.io.*;\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) throws Exception {\n" +
            "        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n" +
            "        String s = br.readLine();\n" +
            "        String t = br.readLine();\n" +
            "        if (s == null) s = \"\";\n" +
            "        if (t == null) t = \"\";\n" +
            "        System.out.print(new Solution().isAnagram(s.trim(), t.trim()));\n" +
            "    }\n" +
            "}\n"
        );

        RUNNER_WRAPPERS.put("Two Sum",
            "import java.io.*;\n" +
            "import java.util.*;\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) throws Exception {\n" +
            "        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n" +
            "        String line1 = br.readLine();\n" +
            "        String line2 = br.readLine();\n" +
            "        if (line1 == null || line2 == null) return;\n" +
            "        line1 = line1.trim();\n" +
            "        if (line1.startsWith(\"[\")) line1 = line1.substring(1);\n" +
            "        if (line1.endsWith(\"]\")) line1 = line1.substring(0, line1.length() - 1);\n" +
            "        int[] nums = Arrays.stream(line1.split(\",\"))\n" +
            "                           .map(String::trim)\n" +
            "                           .mapToInt(Integer::parseInt)\n" +
            "                           .toArray();\n" +
            "        int target = Integer.parseInt(line2.trim());\n" +
            "        int[] res = new Solution().twoSum(nums, target);\n" +
            "        System.out.print(\"[\" + res[0] + \",\" + res[1] + \"]\");\n" +
            "    }\n" +
            "}\n"
        );

        RUNNER_WRAPPERS.put("Best Time to Buy and Sell Stock",
            "import java.io.*;\n" +
            "import java.util.*;\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) throws Exception {\n" +
            "        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n" +
            "        String line = br.readLine();\n" +
            "        if (line == null) return;\n" +
            "        line = line.trim();\n" +
            "        if (line.startsWith(\"[\")) line = line.substring(1);\n" +
            "        if (line.endsWith(\"]\")) line = line.substring(0, line.length() - 1);\n" +
            "        int[] prices = Arrays.stream(line.split(\",\"))\n" +
            "                             .map(String::trim)\n" +
            "                             .mapToInt(Integer::parseInt)\n" +
            "                             .toArray();\n" +
            "        System.out.print(new Solution().maxProfit(prices));\n" +
            "    }\n" +
            "}\n"
        );

        RUNNER_WRAPPERS.put("Valid Parentheses",
            "import java.io.*;\n" +
            "public class Main {\n" +
            "    public static void main(String[] args) throws Exception {\n" +
            "        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n" +
            "        String s = br.readLine();\n" +
            "        if (s == null) s = \"\";\n" +
            "        s = s.trim();\n" +
            "        if (s.startsWith(\"\\\"\") && s.endsWith(\"\\\"\")) s = s.substring(1, s.length() - 1);\n" +
            "        System.out.print(new Solution().isValid(s));\n" +
            "    }\n" +
            "}\n"
        );
    }

    public ExecutionResult evaluate(String problemTitle, String testCasesJson, String userCodeText, String language) {
        try {
            String wrapper = RUNNER_WRAPPERS.get(problemTitle);
            if (wrapper == null) {
                throw new IllegalArgumentException("No runner wrapper template found for problem: " + problemTitle);
            }

            // For non-java languages, we don't have wrappers seeded in Java.
            // If it is Java, we prefix the wrapper runner class.
            String sourceCode;
            if ("java".equalsIgnoreCase(language)) {
                sourceCode = wrapper + "\n" + userCodeText;
            } else {
                sourceCode = userCodeText;
            }

            String base64Source = Base64.getEncoder().encodeToString(sourceCode.getBytes(StandardCharsets.UTF_8));

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

            for (ExecutorExecutionResult res : results) {
                double time = res.getTime() != null ? res.getTime() : 0.0;
                if (time > maxTime) {
                    maxTime = time;
                }

                if (!"ACCEPTED".equalsIgnoreCase(res.getStatus())) {
                    SubmissionStatus failedStatus;
                    try {
                        failedStatus = SubmissionStatus.valueOf(res.getStatus().toUpperCase());
                    } catch (Exception e) {
                        failedStatus = SubmissionStatus.RUNTIME_ERROR;
                    }
                    log.warn("Test case failed execution. Status: {}, Time: {}s", res.getStatus(), time);
                    return new ExecutionResult(failedStatus, (int) (maxTime * 1000));
                }
            }

            return new ExecutionResult(finalStatus, (int) (maxTime * 1000));

        } catch (Exception e) {
            log.error("Execution error occurred during Custom Executor integration: ", e);
            return new ExecutionResult(SubmissionStatus.RUNTIME_ERROR, 0);
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
    }
}
