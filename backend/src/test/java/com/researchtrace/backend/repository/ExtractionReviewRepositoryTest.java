package com.researchtrace.backend.repository;

import com.researchtrace.backend.model.ExtractionReview;
import com.researchtrace.backend.model.ExtractionReviewStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ExtractionReviewRepositoryTest {

    @Autowired
    private ExtractionReviewRepository extractionReviewRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReloadsReviewJsonStatusAndTimestamp() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 12, 22, 15);
        ExtractionReview review = review(21L, ExtractionReviewStatus.ACCEPTED,
            "{\"decisions\":[{\"title\":\"Keep the baseline\"}]}", createdAt);

        Long id = extractionReviewRepository.save(review).getId();
        entityManager.flush();
        entityManager.clear();

        ExtractionReview reloaded = extractionReviewRepository.findById(id).orElseThrow();

        assertThat(reloaded.getMeetingId()).isEqualTo(21L);
        assertThat(reloaded.getStatus()).isEqualTo(ExtractionReviewStatus.ACCEPTED);
        assertThat(reloaded.getExtractionJson()).contains("Keep the baseline");
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void listsReviewsForOneMeetingInCreatedOrder() {
        extractionReviewRepository.save(review(21L, ExtractionReviewStatus.PENDING, "{}",
            LocalDateTime.of(2026, 10, 12, 10, 0)));
        extractionReviewRepository.save(review(21L, ExtractionReviewStatus.EDITED, "{\"edited\":true}",
            LocalDateTime.of(2026, 10, 12, 11, 0)));
        extractionReviewRepository.save(review(22L, ExtractionReviewStatus.REJECTED, "{}",
            LocalDateTime.of(2026, 10, 12, 9, 0)));
        entityManager.flush();
        entityManager.clear();

        List<ExtractionReview> reviews = extractionReviewRepository.findByMeetingIdOrderByCreatedAtAsc(21L);

        assertThat(reviews)
            .extracting(ExtractionReview::getStatus)
            .containsExactly(ExtractionReviewStatus.PENDING, ExtractionReviewStatus.EDITED);
    }

    private ExtractionReview review(
            Long meetingId,
            ExtractionReviewStatus status,
            String json,
            LocalDateTime createdAt) {
        ExtractionReview review = new ExtractionReview();
        review.setMeetingId(meetingId);
        review.setStatus(status);
        review.setExtractionJson(json);
        review.setCreatedAt(createdAt);
        return review;
    }
}
