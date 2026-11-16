package com.researchtrace.backend.ai;

import com.researchtrace.backend.BackendApplication;
import com.researchtrace.backend.ai.MissingGeminiKeyContextInitializer;
import com.researchtrace.backend.research.ResearchNotesExtractor;
import com.researchtrace.backend.research.StubResearchNotesExtractor;
import com.researchtrace.backend.research.UnconfiguredResearchNotesExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MissingGeminiKeyStartupTest {

    @Test
    void applicationStartsAndExtractionReturns503WhenTheGeminiKeyIsMissing() {
        try (ConfigurableApplicationContext context = start(
            "missinggeminikey",
            "--task-extractor.mode=gemini",
            "--research-extractor.mode=gemini",
            "--spring.ai.model.chat=google-genai",
            "--spring.ai.google.genai.api-key="
        )) {
            assertThat(context.getEnvironment().getProperty("spring.ai.model.chat")).isEqualTo("none");

            TaskExtractor taskExtractor = context.getBean(TaskExtractor.class);
            ResearchNotesExtractor researchNotesExtractor = context.getBean(ResearchNotesExtractor.class);
            assertThat(taskExtractor).isInstanceOf(UnconfiguredTaskExtractor.class);
            assertThat(researchNotesExtractor).isInstanceOf(UnconfiguredResearchNotesExtractor.class);

            TaskExtractionInput input = new TaskExtractionInput(
                "Weekly sync", LocalDate.of(2026, 10, 7), "Discuss the baseline.", List.of(), List.of());
            assertThatThrownBy(() -> taskExtractor.extract(input))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception.getReason()).contains("not configured");
                });
            assertThatThrownBy(() -> researchNotesExtractor.extract("Discuss the baseline."))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception.getReason()).contains("not configured");
                });
        }
    }

    @Test
    void explicitStubModeRemainsAvailableWhenTheGeminiKeyIsMissing() {
        try (ConfigurableApplicationContext context = start(
            "stubwithoutkey",
            "--task-extractor.mode=stub",
            "--research-extractor.mode=stub",
            "--spring.ai.model.chat=google-genai",
            "--spring.ai.google.genai.api-key="
        )) {
            assertThat(context.getBean(TaskExtractor.class)).isInstanceOf(StubTaskExtractor.class);
            ResearchNotesExtractor researchNotesExtractor = context.getBean(ResearchNotesExtractor.class);
            assertThat(researchNotesExtractor).isInstanceOf(StubResearchNotesExtractor.class);
            assertThat(researchNotesExtractor.extract("notes")).isNotNull();
        }
    }

    private static ConfigurableApplicationContext start(String databaseName, String... modeArguments) {
        SpringApplication application = new SpringApplication(BackendApplication.class);
        application.addInitializers(new MissingGeminiKeyContextInitializer());
        String[] arguments = new String[modeArguments.length + 5];
        arguments[0] = "--spring.datasource.url=jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1";
        arguments[1] = "--spring.datasource.driver-class-name=org.h2.Driver";
        arguments[2] = "--spring.datasource.username=sa";
        arguments[3] = "--spring.datasource.password=";
        arguments[4] = "--spring.jpa.hibernate.ddl-auto=create-drop";
        System.arraycopy(modeArguments, 0, arguments, 5, modeArguments.length);
        return application.run(arguments);
    }
}
