package com.researchtrace.backend.service;

import com.researchtrace.backend.dto.MeetingExtractionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class MeetingExtractionService {

    private final ChatClient chatClient;

    public MeetingExtractionService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public MeetingExtractionResponse extractFromMeetingNotes(String meetingNotes) {

        String systemPrompt = """
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

        return chatClient.prompt()
                .system(systemPrompt)
                .user(meetingNotes)
                .call()
                .entity(MeetingExtractionResponse.class);
    }
}