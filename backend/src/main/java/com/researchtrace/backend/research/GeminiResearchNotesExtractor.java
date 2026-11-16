package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class GeminiResearchNotesExtractor implements ResearchNotesExtractor {

    static final String SYSTEM_PROMPT = """
            You are an AI assistant for a research project supervision system.

            Extract structured information from research meeting notes.

            Identify:
            1. Tasks
            2. Decisions
            3. Experiments
            4. Results
            5. Deadlines
            6. Research entities
            7. Possible research direction changes

            Only extract information that is supported by the meeting notes.
            Do not invent information.
            For an experiment, set method only when the notes explicitly describe how it will be carried out.
            For a result, set comparison only when the notes explicitly compare it with another result or baseline.
            For a decision, set madeBy only when the notes explicitly name who made it.

            For every extracted item, provide a confidence value between 0 and 1.

            If a field is not explicitly available in the meeting notes, use null.
            """;

    private final ChatClient chatClient;

    public GeminiResearchNotesExtractor(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public MeetingExtractionResponse extract(String meetingNotes) {
        ResearchExtractionChecks.requireNotes(meetingNotes);
        MeetingExtractionResponse response;
        try {
            response = callModel(meetingNotes);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Gemini research extraction failed");
        }
        ResearchExtractionChecks.validateModelResponse(response);
        return response;
    }

    MeetingExtractionResponse callModel(String meetingNotes) {
        return chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(meetingNotes)
            .call()
            .entity(MeetingExtractionResponse.class);
    }
}
