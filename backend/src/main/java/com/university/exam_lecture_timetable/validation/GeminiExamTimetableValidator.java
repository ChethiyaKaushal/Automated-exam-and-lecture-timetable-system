package com.university.exam_lecture_timetable.validation;

import com.university.exam_lecture_timetable.dto.request.StartAssignment;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.Draft;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Review;
import com.university.exam_lecture_timetable.exception.ExamTimetableReviewException;
import com.university.exam_lecture_timetable.service.ExamSchedulingInputService.DraftInput;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

public final class GeminiExamTimetableValidator {

    private GeminiExamTimetableValidator() {}

    public static Review review(DraftInput loaded, JsonNode proposal) {
        JsonNode entries = proposal.path("exams");
        String status = proposal.path("status").asText("");
        if ("UNSCHEDULED".equals(status) && entries.isArray() && entries.isEmpty()) {
            throw invalid("Gemini did not find a complete schedule.");
        }
        if (!"SCHEDULED".equals(status) || !proposal.path("reason").isNull()
                || !entries.isArray() || entries.size() != loaded.input().exams().size()) {
            throw invalid("Expected one scheduled entry per exam.");
        }

        List<StartAssignment> assignments = new ArrayList<>();
        for (JsonNode entry : entries) {
            JsonNode venueIds = entry.path("venueIds");
            if (!venueIds.isArray() || venueIds.isEmpty()) {
                throw invalid("Each exam needs at least one venue.");
            }
            List<Long> venues = new ArrayList<>();
            for (JsonNode venueId : venueIds) {
                venues.add(positiveId(venueId));
            }
            assignments.add(new StartAssignment(positiveId(entry.path("requirementId")),
                    positiveId(entry.path("startTimeslotId")), List.copyOf(venues)));
        }

        return ExamTimetableReviewValidator.review(loaded, assignments);
    }

    public static Draft toDraft(DraftInput loaded, Review review) {
        if (!review.valid()) {
            var reasons = review.issues().stream().filter(issue -> "ERROR".equals(issue.severity()))
                    .map(issue -> issue.description()).limit(3).toList();
            throw invalid(String.join(" ", reasons));
        }
        return new Draft(loaded.input().periodId(), "DRAFT", loaded.membershipComplete(), true,
                review.exams(), List.of());
    }

    private static long positiveId(JsonNode node) {
        if (!node.isIntegralNumber() || !node.canConvertToLong() || node.asLong() <= 0) {
            throw invalid("IDs must be positive whole numbers.");
        }
        return node.asLong();
    }

    private static ExamTimetableReviewException invalid(String reason) {
        return new ExamTimetableReviewException("Gemini timetable rejected: " + reason);
    }
}
