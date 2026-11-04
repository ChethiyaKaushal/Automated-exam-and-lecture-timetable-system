package com.university.exam_lecture_timetable.service;

import static org.junit.jupiter.api.Assertions.*;

import com.university.exam_lecture_timetable.service.ExamSchedulingInput.Conflict;
import com.university.exam_lecture_timetable.service.ExamSchedulingInput.Exam;
import com.university.exam_lecture_timetable.service.ExamSchedulingInput.Group;
import com.university.exam_lecture_timetable.service.ExamSchedulingInput.Slot;
import com.university.exam_lecture_timetable.service.ExamSchedulingInput.Venue;
import com.university.exam_lecture_timetable.service.ExamTimetableReviewService.StartAssignment;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExamTimetableReviewServiceTests {

    @Test
    void rejectsKnownStudentClashesAndWarnsAboutSameDayWorkload() {
        var loaded = new ExamSchedulingInputService.DraftInput(new ExamSchedulingInput(1L,
                List.of(exam(1, "AA1201", "Exam A", 120), exam(2, "AA1202", "Exam B", 120)),
                slots(), List.of(room(1, "DO1"), room(2, "DO2")), List.of(new Conflict(1L, 2L))), true);

        var overlapping = ExamTimetableReviewService.review(loaded,
                List.of(new StartAssignment(1L, 1L), new StartAssignment(2L, 1L)));
        assertFalse(overlapping.valid());
        assertTrue(overlapping.issues().stream().anyMatch(issue -> issue.code().equals("STUDENT_OR_GROUP_CLASH")));

        var spaced = ExamTimetableReviewService.review(loaded,
                List.of(new StartAssignment(1L, 1L), new StartAssignment(2L, 7L)));
        assertTrue(spaced.valid());
        assertTrue(spaced.issues().stream().anyMatch(issue -> issue.code().equals("SAME_DAY_WORKLOAD")));

        var lunch = ExamTimetableReviewService.review(loaded,
                List.of(new StartAssignment(1L, 6L), new StartAssignment(2L, 7L)));
        assertFalse(lunch.valid());
        assertTrue(lunch.issues().stream().anyMatch(issue -> issue.code().equals("EXAM_DOES_NOT_FIT")));
    }

    @Test
    void drawingGroupsMustShareOneDayAndKeepTheirSessionsAndRooms() {
        var names = List.of("A-I", "A-II", "B-I", "B-II");
        List<Exam> exams = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            exams.add(exam(i + 1, "ME1201", "Engineering Drawing (Group " + names.get(i) + ")", 180));
        }
        var loaded = new ExamSchedulingInputService.DraftInput(new ExamSchedulingInput(1L,
                exams, slots(), List.of(room(1, "DO1"), room(2, "DO2")), List.of()), false);
        var assignments = List.of(new StartAssignment(1L, 1L), new StartAssignment(2L, 1L),
                new StartAssignment(3L, 7L), new StartAssignment(4L, 7L));
        var valid = ExamTimetableReviewService.review(loaded, assignments);
        assertTrue(valid.valid());
        assertEquals(List.of("DO1", "DO2", "DO1", "DO2"),
                valid.exams().stream().map(exam -> exam.venues().getFirst().venueName()).toList());

        var moved = new ArrayList<>(assignments);
        moved.set(3, new StartAssignment(4L, 19L));
        var result = ExamTimetableReviewService.review(loaded, moved);
        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.code().equals("DRAWING_SESSION")));
    }

    private static Exam exam(long id, String code, String name, int duration) {
        return new Exam(id, id, code, name, "C-23", duration, 40, List.of(new Group(id, 40)));
    }

    private static Venue room(long id, String name) {
        return new Venue(id, name, 140, List.of());
    }

    private static List<Slot> slots() {
        List<Slot> result = new ArrayList<>();
        for (String date : List.of("2026-12-01", "2026-12-02")) {
            for (LocalTime start : List.of(LocalTime.of(9, 0), LocalTime.of(13, 30))) {
                for (int i = 0; i < 6; i++) {
                    LocalTime time = start.plusMinutes(30L * i);
                    result.add(new Slot((long) result.size() + 1, date, time.toString(), time.plusMinutes(30).toString()));
                }
            }
        }
        return result;
    }
}
