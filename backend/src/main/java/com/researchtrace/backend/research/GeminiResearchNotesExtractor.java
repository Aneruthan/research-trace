package com.researchtrace.backend.research;

import com.researchtrace.backend.dto.research.MeetingExtractionResponse;
import org.springframework.ai.chat.client.ChatClient;

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

            For every extracted item, provide a confidence value between 0 and 1.

            If a field is not explicitly available in the meeting notes, use null.
            """;

    private final ChatClient chatClient;

    public GeminiResearchNotesExtractor(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public MeetingExtractionResponse extract(String meetingNotes) {
        return chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(meetingNotes)
            .call()
            .entity(MeetingExtractionResponse.class);
    }
}
