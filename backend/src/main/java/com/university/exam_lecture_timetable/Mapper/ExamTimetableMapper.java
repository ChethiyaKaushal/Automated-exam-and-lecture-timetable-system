package com.university.exam_lecture_timetable.Mapper;

import com.university.exam_lecture_timetable.dto.request.StartAssignment;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.Draft;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.ScheduledExam;
import com.university.exam_lecture_timetable.entity.EventAllocationEntity;
import com.university.exam_lecture_timetable.entity.EventEntity;
import com.university.exam_lecture_timetable.entity.EventRequirementEntity;
import com.university.exam_lecture_timetable.entity.EventSlotEntity;
import com.university.exam_lecture_timetable.entity.StudentGroupAllocationEntity;
import com.university.exam_lecture_timetable.entity.StudentGroupEntity;
import com.university.exam_lecture_timetable.entity.TimeslotEntity;
import com.university.exam_lecture_timetable.entity.TimetableEntity;
import com.university.exam_lecture_timetable.entity.VenueEntity;
import jakarta.persistence.EntityManager;
import java.util.List;

public final class ExamTimetableMapper {

    private ExamTimetableMapper() {}

    public static List<StartAssignment> starts(Draft draft) {
        return java.util.stream.Stream.concat(
                draft.exams().stream().map(exam -> new StartAssignment(exam.requirementId(), exam.timeslotIds().getFirst())),
                draft.unscheduledExams().stream().map(exam -> new StartAssignment(exam.requirementId(), null)))
                .toList();
    }

    public static EventEntity toEvent(EntityManager em, TimetableEntity timetable, ScheduledExam scheduled) {
        var event = new EventEntity();
        event.setTimetable(timetable);
        event.setEventType("EXAM");
        event.setRequirement(em.getReference(EventRequirementEntity.class, scheduled.requirementId()));
        for (Long slotId : scheduled.timeslotIds()) {
            var slot = new EventSlotEntity();
            slot.setEvent(event);
            slot.setTimeslot(em.getReference(TimeslotEntity.class, slotId));
            event.getEventSlots().add(slot);
        }
        for (var room : scheduled.venues()) {
            var allocation = new EventAllocationEntity();
            allocation.setEvent(event);
            allocation.setVenue(em.getReference(VenueEntity.class, room.venueId()));
            for (var group : room.groups()) {
                var seating = new StudentGroupAllocationEntity();
                seating.setEventAllocation(allocation);
                seating.setStudentGroup(em.getReference(StudentGroupEntity.class, group.groupId()));
                seating.setAllocatedCount(group.allocatedCount());
                allocation.getStudentGroups().add(seating);
            }
            event.getAllocations().add(allocation);
        }
        return event;
    }
}
