package com.researchtrace.backend.ai;

import com.researchtrace.backend.ai.TaskExtractionInput.MilestoneCandidate;
import com.researchtrace.backend.ai.TaskExtractionInput.StudentCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class LlmTaskExtractorTest {

    private static final String API_KEY = "test-key";
    private static final String MODEL = "gpt-4o-mini";
    private static final String BASE_URL = "https://api.openai.com";

    private static final List<StudentCandidate> STUDENTS = List.of(
        new StudentCandidate(7L, "Aneruthan"),
        new StudentCandidate(8L, "Gayathri"));

    private static final List<MilestoneCandidate> MILESTONES = List.of(
        new MilestoneCandidate(11L, "Baseline evaluation"),
        new MilestoneCandidate(12L, "Dataset preparation"));

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private LlmTaskExtractor extractor;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        extractor = new LlmTaskExtractor(restClientBuilder.build(), API_KEY, MODEL);
    }

    @Test
    void extractsAValidStructuredResponse() {
        expectChatCompletion(chatCompletion("""
            {"tasks":[{
              "title":"Run Qwen baseline",
              "description":"Evaluate on the dev split",
              "assigneeName":"Aneruthan",
              "assigneeId":7,
              "milestoneId":11,
              "dueDate":"2026-10-05",
              "sourceExcerpt":"Aneruthan will run the Qwen baseline by Friday",
              "confidence":0.85
            }]}
            """));

        List<ExtractedTask> tasks = extractor.extract(sampleInput());

        assertThat(tasks).containsExactly(new ExtractedTask(
            "Run Qwen baseline",
            "Evaluate on the dev split",
            "Aneruthan",
            7L,
            11L,
            LocalDate.of(2026, 10, 5),
            "Aneruthan will run the Qwen baseline by Friday",
            new BigDecimal("0.85")));
        server.verify();
    }

    @Test
    void extractsMultipleTasksInOrder() {
        expectChatCompletion(chatCompletion("""
            {"tasks":[
              {"title":"Run Qwen baseline","assigneeId":7,"confidence":0.9},
              {"title":"Prepare dataset statistics","assigneeId":8,"confidence":0.8}
            ]}
            """));

        assertThat(extractor.extract(sampleInput()))
            .extracting(ExtractedTask::title, ExtractedTask::assigneeId)
            .containsExactly(
                tuple("Run Qwen baseline", 7L),
                tuple("Prepare dataset statistics", 8L));
        server.verify();
    }

    @Test
    void mapsMissingOptionalFieldsToNull() {
        expectChatCompletion(chatCompletion("""
            {"tasks":[{"title":"Write related work"}]}
            """));

        assertThat(extractor.extract(sampleInput())).singleElement().satisfies(task -> {
            assertThat(task.title()).isEqualTo("Write related work");
            assertThat(task.description()).isNull();
            assertThat(task.assigneeName()).isNull();
            assertThat(task.assigneeId()).isNull();
            assertThat(task.milestoneId()).isNull();
            assertThat(task.dueDate()).isNull();
            assertThat(task.sourceExcerpt()).isNull();
            assertThat(task.confidence()).isNull();
        });
        server.verify();
    }

    @Test
    void rejectsMalformedJsonFromTheModel() {
        expectChatCompletion(chatCompletion("{not json"));

        assertThatThrownBy(() -> extractor.extract(sampleInput()))
            .isInstanceOf(TaskExtractionException.class)
            .hasMessageContaining("malformed JSON");
        server.verify();
    }

    @Test
    void treatsInvalidDatesAsNull() {
        expectChatCompletion(chatCompletion("""
            {"tasks":[{"title":"Run Qwen baseline","dueDate":"next Friday"}]}
            """));

        assertThat(extractor.extract(sampleInput())).singleElement().satisfies(task -> {
            assertThat(task.title()).isEqualTo("Run Qwen baseline");
            assertThat(task.dueDate()).isNull();
        });
        server.verify();
    }

    @Test
    void treatsInvalidConfidenceAsNull() {
        expectChatCompletion(chatCompletion("""
            {"tasks":[
              {"title":"Too high","confidence":1.5},
              {"title":"Negative","confidence":-0.1},
              {"title":"Not a number","confidence":"high"},
              {"title":"Valid","confidence":0.4}
            ]}
            """));

        assertThat(extractor.extract(sampleInput()))
            .extracting(ExtractedTask::title, ExtractedTask::confidence)
            .containsExactly(
                tuple("Too high", null),
                tuple("Negative", null),
                tuple("Not a number", null),
                tuple("Valid", new BigDecimal("0.4")));
        server.verify();
    }

    @Test
    void failsWhenApiKeyIsMissing() {
        extractor = new LlmTaskExtractor(restClientBuilder.build(), "  ", MODEL);

        assertThatThrownBy(() -> extractor.extract(sampleInput()))
            .isInstanceOf(TaskExtractionException.class)
            .hasMessageContaining("OPENAI_API_KEY");
        server.verify();
    }

    @Test
    void failsWhenModelIsMissing() {
        extractor = new LlmTaskExtractor(restClientBuilder.build(), API_KEY, "");

        assertThatThrownBy(() -> extractor.extract(sampleInput()))
            .isInstanceOf(TaskExtractionException.class)
            .hasMessageContaining("OPENAI_MODEL");
        server.verify();
    }

    @Test
    void failsWhenTheApiReturnsAnError() {
        server.expect(requestTo(BASE_URL + "/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError());

        assertThatThrownBy(() -> extractor.extract(sampleInput()))
            .isInstanceOf(TaskExtractionException.class)
            .hasMessageContaining("HTTP 500");
        server.verify();
    }

    @Test
    void failsWhenTheApiRejectsTheKey() {
        server.expect(requestTo(BASE_URL + "/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> extractor.extract(sampleInput()))
            .isInstanceOf(TaskExtractionException.class)
            .hasMessageContaining("HTTP 401");
        server.verify();
    }

    @Test
    void requestContainsMeetingTitleNotesStudentsAndMilestones() {
        server.expect(requestTo(BASE_URL + "/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer " + API_KEY))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().string(allOf(
                containsString("\"model\":\"" + MODEL + "\""),
                containsString("Weekly sync"),
                containsString("2026-10-02"),
                containsString("Aneruthan will run the Qwen baseline by Friday"),
                containsString("Students (id and name)"),
                containsString("Aneruthan"),
                containsString("Gayathri"),
                containsString("Milestones (id and title)"),
                containsString("Baseline evaluation"))))
            .andRespond(withSuccess(chatCompletion("{\"tasks\":[]}"), MediaType.APPLICATION_JSON));

        assertThat(extractor.extract(sampleInput())).isEmpty();
        server.verify();
    }

    @Test
    void returnsEmptyListForEmptyModelOutput() {
        expectChatCompletion(chatCompletion("{\"tasks\":[]}"));

        assertThat(extractor.extract(sampleInput())).isEmpty();
        server.verify();
    }

    @Test
    void skipsTasksWithoutATitle() {
        expectChatCompletion(chatCompletion("""
            {"tasks":[
              {"description":"No title"},
              {"title":"  "},
              {"title":"Keep this task"}
            ]}
            """));

        assertThat(extractor.extract(sampleInput()))
            .extracting(ExtractedTask::title)
            .containsExactly("Keep this task");
        server.verify();
    }

    @Test
    void hasNoRepositoryOrDatabaseDependency() {
        for (Field field : LlmTaskExtractor.class.getDeclaredFields()) {
            assertThat(Repository.class.isAssignableFrom(field.getType())).isFalse();
            assertThat(field.getType().getPackageName())
                .doesNotStartWith("com.researchtrace.backend.repository")
                .doesNotStartWith("com.researchtrace.backend.model");
        }
        assertThat(LlmTaskExtractor.class.getPackageName())
            .doesNotStartWith("com.researchtrace.backend.repository");
    }

    private void expectChatCompletion(String responseJson) {
        server.expect(requestTo(BASE_URL + "/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer " + API_KEY))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private String chatCompletion(String contentJson) {
        String escaped = contentJson
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n");
        return """
            {"choices":[{"message":{"role":"assistant","content":"%s"}}]}
            """.formatted(escaped);
    }

    private TaskExtractionInput sampleInput() {
        return new TaskExtractionInput(
            "Weekly sync",
            LocalDate.of(2026, 10, 2),
            "Aneruthan will run the Qwen baseline by Friday",
            STUDENTS,
            MILESTONES);
    }
}
