package com.codeduel.codeduel.submission.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
public class Judge0service {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${judge0.url:http://localhost:2358}")
    private String judge0Url;

    @Value("${judge0.language-id:62}") // Default to 62 (Java 13+) or 91 (Java 17) depending on container
    private int languageId;

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

    public ExecutionResult evaluate(String problemTitle, String testCasesJson, String userCodeText) {
        try {
            
            String wrapper = RUNNER_WRAPPERS.get(problemTitle);
            if (wrapper == null) {
                throw new IllegalArgumentException("No runner wrapper template found for problem: " + problemTitle);
            }

            // 
            String sourceCode = wrapper + "\n" + userCodeText;
            String base64Source = Base64.getEncoder().encodeToString(sourceCode.getBytes(StandardCharsets.UTF_8));

            
            log.info("Raw test cases JSON from DB: {}", testCasesJson);
            List<TestCase> testCases = objectMapper.readValue(testCasesJson, new TypeReference<List<TestCase>>() {});
            if (testCases == null || testCases.isEmpty()) {
                throw new IllegalStateException("Problem has no test cases loaded.");
            }
            log.info("Parsed test cases count: {}", testCases.size());
            
            List<Judge0SubmissionRequest> submissions = testCases.stream().map(tc -> {
                String base64Input = Base64.getEncoder().encodeToString(tc.getInput().getBytes(StandardCharsets.UTF_8));
                String base64ExpectedOutput = Base64.getEncoder().encodeToString(tc.getOutput().getBytes(StandardCharsets.UTF_8));
                return new Judge0SubmissionRequest(base64Source, languageId, base64Input, base64ExpectedOutput);
            }).collect(Collectors.toList());

            Judge0BatchRequest requestBody = new Judge0BatchRequest(submissions);
            log.info("Request payload: {}", objectMapper.writeValueAsString(requestBody));

            
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            String submitUrl = judge0Url + "/submissions/batch?base64_encoded=true";
            log.info("Sending batch submissions to Judge0 API at: {}", submitUrl);

            ResponseEntity<List<Map<String, String>>> responseEntity = restTemplate.exchange(
                submitUrl,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<List<Map<String, String>>>() {}
            );

            List<Map<String, String>> responseList = responseEntity.getBody();
            if (responseList == null || responseList.isEmpty()) {
                throw new RuntimeException("Failed to obtain execution tokens from Judge0.");
            }

            List<String> tokens = responseList.stream()
                .map(m -> m.get("token"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            
            return pollAndAggregateResults(tokens);

        } catch (Exception e) {
            log.error("Execution error occurred during Judge0 integration: ", e);
            return new ExecutionResult(SubmissionStatus.RUNTIME_ERROR, 0);
        }
    }

    private ExecutionResult pollAndAggregateResults(List<String> tokens) throws Exception {
        String tokenCsv = String.join(",", tokens);
        String pollUrl = judge0Url + "/submissions/batch?tokens=" + tokenCsv + "&base64_encoded=true&fields=status,time,stderr,compile_output";

        int maxAttempts = 15;
        int attempt = 0;
        List<Judge0ExecutionResult> results = new ArrayList<>();

        while (attempt < maxAttempts) {
            log.info("Polling Judge0 token statuses, attempt {}/{}", attempt + 1, maxAttempts);
            ResponseEntity<Judge0BatchPollResponse> responseEntity = restTemplate.getForEntity(pollUrl, Judge0BatchPollResponse.class);
            Judge0BatchPollResponse body = responseEntity.getBody();

            if (body != null && body.getSubmissions() != null) {
                results = body.getSubmissions();
                boolean allFinished = results.stream().allMatch(res -> res.getStatus() != null && res.getStatus().getId() > 2);
                if (allFinished) {
                    break;
                }
            }

            attempt++;
            Thread.sleep(1000); // Wait 1 second before next poll
        }

        if (results.isEmpty()) {
            throw new RuntimeException("Obtained empty response while polling execution results.");
        }

        // 7. Aggregate statuses
        SubmissionStatus finalStatus = SubmissionStatus.ACCEPTED;
        double maxTime = 0.0;

        for (Judge0ExecutionResult res : results) {
            if (res.getStatus() == null) {
                continue;
            }

            int statusId = res.getStatus().getId();
            double time = res.getTime() != null ? res.getTime() : 0.0;
            if (time > maxTime) {
                maxTime = time;
            }

            if (statusId != 3) { // 3 is Accepted
                SubmissionStatus failedStatus = mapStatus(statusId);
                log.warn("Test case failed execution. Status: {}, Time: {}s", res.getStatus().getDescription(), time);
                // Prioritize WRONG_ANSWER, TLE, or COMPILE errors
                return new ExecutionResult(failedStatus, (int) (maxTime * 1000));
            }
        }

        return new ExecutionResult(finalStatus, (int) (maxTime * 1000));
    }

    private SubmissionStatus mapStatus(int judge0StatusId) {
        return switch (judge0StatusId) {
            case 3 -> SubmissionStatus.ACCEPTED;
            case 4 -> SubmissionStatus.WRONG_ANSWER;
            case 5 -> SubmissionStatus.TLE;
            case 6 -> SubmissionStatus.COMPILATION_ERROR;
            default -> SubmissionStatus.RUNTIME_ERROR;
        };
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
    public static class Judge0SubmissionRequest {
        private String source_code;
        private int language_id;
        private String stdin;
        private String expected_output;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Judge0BatchRequest {
        private List<Judge0SubmissionRequest> submissions;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Judge0Status {
        private int id;
        private String description;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Judge0ExecutionResult {
        private String token;
        private Judge0Status status;
        private Double time;
        private String compile_output;
        private String stderr;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Judge0BatchPollResponse {
        private List<Judge0ExecutionResult> submissions;
    }

    @Getter
    @AllArgsConstructor
    public static class ExecutionResult {
        private final SubmissionStatus status;
        private final int executionTimeMs;
    }
}
