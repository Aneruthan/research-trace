package com.researchtrace.backend.ai;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible TaskExtractor. Returns candidate tasks as plain data and does
 * not access repositories or persist suggestions.
 */
public class LlmTaskExtractor implements TaskExtractor {

    static final int MAX_NOTES_CHARS = 20_000;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private static final String SYSTEM_PROMPT = """
        You extract actionable research-project tasks from meeting notes.
        Return JSON only, with this exact shape:
        {"tasks":[{"title":string,"description":string|null,"assigneeName":string|null,"assigneeId":number|null,"milestoneId":number|null,"dueDate":"YYYY-MM-DD"|null,"sourceExcerpt":string|null,"confidence":number|null}]}
        Rules:
        - Include only tasks clearly supported by the notes.
        - title is required; skip items without a title.
        - assigneeId must be one of the provided student ids, or null.
        - milestoneId must be one of the provided milestone ids, or null.
        - dueDate must be ISO-8601 YYYY-MM-DD or null. Do not invent dates.
        - confidence is a number from 0 to 1, or null.
        - sourceExcerpt is a short quote copied from the notes, or null.
        Do not create, update, or delete any database records.
        """;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final JsonMapper jsonMapper;

    public LlmTaskExtractor(TaskExtractorProperties properties) {
        this(defaultClient(requireBaseUrl(properties)),
            properties.getOpenai().getApiKey(),
            properties.getOpenai().getModel(),
            JsonMapper.builder().build());
    }

    LlmTaskExtractor(RestClient restClient, String apiKey, String model) {
        this(restClient, apiKey, model, JsonMapper.builder().build());
    }

    LlmTaskExtractor(RestClient restClient, String apiKey, String model, JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.jsonMapper = jsonMapper;
    }

    @Override
    public List<ExtractedTask> extract(TaskExtractionInput input) {
        if (input == null) {
            return List.of();
        }
        requireConfiguration();

        String responseBody = callModel(buildRequestBody(input));
        return parseTasks(readMessageContent(responseBody));
    }

    private void requireConfiguration() {
        if (apiKey.isEmpty()) {
            throw new TaskExtractionException("OPENAI_API_KEY is not configured");
        }
        if (model.isEmpty()) {
            throw new TaskExtractionException("OPENAI_MODEL is not configured");
        }
    }

    private String buildRequestBody(TaskExtractionInput input) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
            Map.of("role", "system", "content", SYSTEM_PROMPT),
            Map.of("role", "user", "content", userPrompt(input))));
        try {
            return jsonMapper.writeValueAsString(body);
        } catch (RuntimeException e) {
            throw new TaskExtractionException("Could not build the OpenAI request", e);
        }
    }

    private String userPrompt(TaskExtractionInput input) {
        String notes = input.notes() == null ? "" : input.notes();
        if (notes.length() > MAX_NOTES_CHARS) {
            notes = notes.substring(0, MAX_NOTES_CHARS);
        }
        return """
            Meeting title: %s
            Meeting date: %s
            Students (id and name): %s
            Milestones (id and title): %s
            Notes:
            %s
            """.formatted(
            nullToEmpty(input.meetingTitle()),
            input.meetingDate() == null ? "" : input.meetingDate().toString(),
            writeJson(input.students()),
            writeJson(input.milestones()),
            notes);
    }

    private String callModel(String requestJson) {
        try {
            String body = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(requestJson)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                    throw new TaskExtractionException(
                        "OpenAI API returned HTTP " + httpResponse.getStatusCode().value());
                })
                .body(String.class);
            if (body == null || body.isBlank()) {
                throw new TaskExtractionException("OpenAI API returned an empty response");
            }
            return body;
        } catch (TaskExtractionException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new TaskExtractionException(
                "OpenAI API returned HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new TaskExtractionException("OpenAI API request failed", e);
        }
    }

    private String readMessageContent(String responseBody) {
        JsonNode root = readJson(responseBody, "OpenAI API returned malformed JSON");
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new TaskExtractionException("OpenAI API returned no message content");
        }
        return stripMarkdownFence(content.asString());
    }

    private List<ExtractedTask> parseTasks(String content) {
        JsonNode root = readJson(content, "LLM returned malformed JSON");
        JsonNode tasksNode = root.path("tasks");
        if (tasksNode.isMissingNode() || tasksNode.isNull()) {
            return List.of();
        }
        if (!tasksNode.isArray()) {
            throw new TaskExtractionException("LLM returned invalid model output");
        }

        List<ExtractedTask> tasks = new ArrayList<>();
        for (JsonNode node : tasksNode) {
            ExtractedTask task = toExtractedTask(node);
            if (task != null) {
                tasks.add(task);
            }
        }
        return List.copyOf(tasks);
    }

    private ExtractedTask toExtractedTask(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String title = textOrNull(node, "title");
        if (title == null) {
            return null;
        }
        return new ExtractedTask(
            title,
            textOrNull(node, "description"),
            textOrNull(node, "assigneeName"),
            longOrNull(node, "assigneeId"),
            longOrNull(node, "milestoneId"),
            dateOrNull(node, "dueDate"),
            textOrNull(node, "sourceExcerpt"),
            confidenceOrNull(node.get("confidence")));
    }

    private JsonNode readJson(String json, String errorMessage) {
        try {
            return jsonMapper.readTree(json);
        } catch (RuntimeException e) {
            throw new TaskExtractionException(errorMessage, e);
        }
    }

    private String writeJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (RuntimeException e) {
            throw new TaskExtractionException("Could not serialize extraction context", e);
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        String text = value.asString();
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long longOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (value.isIntegralNumber()) {
            return value.asLong();
        }
        if (value.isString()) {
            try {
                return Long.valueOf(value.asString().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private LocalDate dateOrNull(JsonNode node, String field) {
        String text = textOrNull(node, field);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private BigDecimal confidenceOrNull(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        BigDecimal confidence;
        try {
            if (value.isNumber()) {
                confidence = value.decimalValue();
            } else if (value.isString()) {
                confidence = new BigDecimal(value.asString().trim());
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
            return null;
        }
        return confidence;
    }

    private String stripMarkdownFence(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
        trimmed = trimmed.replaceFirst("\\s*```$", "");
        return trimmed.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String requireBaseUrl(TaskExtractorProperties properties) {
        String baseUrl = properties.getOpenai().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static RestClient defaultClient(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build();
    }
}
