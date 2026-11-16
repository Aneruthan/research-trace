package com.researchtrace.backend.controller;

import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import com.researchtrace.backend.research.ExtractionReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExtractionReviewController.class)
class ExtractionReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExtractionReviewService extractionReviewService;

    @Test
    void submitsAndListsReviews() throws Exception {
        ExtractionReview review = new ExtractionReview();
        review.setId(5L);
        review.setMeetingId(21L);
        review.setStatus(ExtractionReviewStatus.ACCEPTED);
        review.setExtractionJson("{\"decisions\":[{\"title\":\"Keep the baseline\"}]}");
        review.setCreatedAt(LocalDateTime.of(2026, 10, 12, 22, 15));
        when(extractionReviewService.reviewExtraction(eq(1L), eq(21L), any())).thenReturn(review);
        when(extractionReviewService.getReviewsForMeeting(1L, 21L)).thenReturn(List.of(review));

        mockMvc.perform(post("/api/projects/1/meetings/21/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "ACCEPTED",
                      "extraction": {
                        "decisions": [{"title": "Keep the baseline", "confidence": 0.9}]
                      }
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.meetingId").value(21))
            .andExpect(jsonPath("$.extractionJson").value(review.getExtractionJson()));

        mockMvc.perform(get("/api/projects/1/meetings/21/reviews"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
    }

    @Test
    void rejectsMissingStatusOrExtraction() throws Exception {
        mockMvc.perform(post("/api/projects/1/meetings/21/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"extraction\": {}}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/projects/1/meetings/21/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"PENDING\"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/projects/1/meetings/21/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"MAYBE\", \"extraction\": {}}"))
            .andExpect(status().isBadRequest());

        verify(extractionReviewService, never()).reviewExtraction(any(), any(), any());
    }

    @Test
    void returnsNotFoundWhenMeetingIsMissing() throws Exception {
        when(extractionReviewService.getReviewsForMeeting(1L, 99L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        mockMvc.perform(get("/api/projects/1/meetings/99/reviews"))
            .andExpect(status().isNotFound());
    }
}
