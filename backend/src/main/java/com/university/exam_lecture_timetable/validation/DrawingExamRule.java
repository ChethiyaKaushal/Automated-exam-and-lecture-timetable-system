package com.university.exam_lecture_timetable.validation;

import com.university.exam_lecture_timetable.dto.ExamSchedulingInput;
import com.university.exam_lecture_timetable.dto.ExamSchedulingInput.Exam;
import com.university.exam_lecture_timetable.exception.ExamTimetableReviewException;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class DrawingExamRule {

    private DrawingExamRule() {}

    public static List<Assignment> assignments(ExamSchedulingInput input) {
        boolean hasDrawing = input.exams().stream().anyMatch(DrawingExamRule::isDrawing);
        if (!hasDrawing) {
            return List.of();
        }
        if (input.exams().stream().filter(DrawingExamRule::isDrawing).count() != 4) {
            throw new ExamTimetableReviewException("ME1201 C-23 requires exactly four Engineering Drawing exam groups.");
        }

        return List.of(
                assignment(input, "A-I", "DO1", "09:00"),
                assignment(input, "A-II", "DO2", "09:00"),
                assignment(input, "B-I", "DO1", "13:30"),
                assignment(input, "B-II", "DO2", "13:30"));
    }

    private static boolean isDrawing(Exam exam) {
        return "ME1201".equals(exam.moduleCode()) && "C-23".equals(exam.curriculum());
    }

    private static Assignment assignment(ExamSchedulingInput input, String group, String venue, String start) {
        String name = "Engineering Drawing (Group " + group + ")";
        var exams = input.exams().stream()
                .filter(DrawingExamRule::isDrawing)
                .filter(exam -> name.equals(exam.moduleName()))
                .toList();
        var venues = input.venues().stream().filter(room -> venue.equals(room.venueName())).toList();
        if (exams.size() != 1 || exams.getFirst().durationMinutes() != 180 || venues.size() != 1) {
            throw new ExamTimetableReviewException("ME1201 C-23 " + group + " needs one three-hour exam and venue " + venue + ".");
        }
        return new Assignment(exams.getFirst(), venues.getFirst(), LocalTime.parse(start));
    }

    public record Assignment(Exam exam, ExamSchedulingInput.Venue venue, LocalTime start) {}
}
