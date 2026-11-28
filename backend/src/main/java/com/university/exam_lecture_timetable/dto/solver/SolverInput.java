package com.university.exam_lecture_timetable.dto.solver;

import com.university.exam_lecture_timetable.entity.VenueType;

import java.util.List;

public record SolverInput(
        Long periodId,
        List<Exam> exams,
        List<Slot> timeslots,
        List<Venue> venues,
        List<ConflictPair> conflictPairs,
        List<SharedExamGroup> sharedExamGroups,
        List<FixedAssignment> fixedAssignments
) {
    public record Exam(
            Long requirementId,
            Long moduleId,
            String moduleCode,
            String moduleName,
            String curriculum,
            Long departmentId,
            Long semesterId,
            int durationMinutes,
            int examGapDays,
            VenueType requiredVenueType,
            int studentCount,
            List<Group> groups
    ) {}

    public record Group(Long groupId, int studentCount) {}

    public record Slot(Long timeslotId, String date, String startTime, String endTime) {}

    public record Venue(
            Long venueId,
            String venueName,
            VenueType venueType,
            int examCapacity,
            int priority,
            List<Long> unavailableTimeslotIds
    ) {}

    public enum ConflictType {
        /** A common student takes both exams as PROPER: the exams must be on different days. */
        DIFFERENT_DAY,
        /** Any other common student: the exams must not overlap, but may be on the same day. */
        NO_TIME_OVERLAP
    }

    public record ConflictPair(
            Long firstRequirementId,
            Long secondRequirementId,
            ConflictType conflictType,
            boolean firstHasProperStudents,
            boolean secondHasProperStudents
    ) {

        public static ConflictPair canonical(
                Long firstRequirementId, Long secondRequirementId, ConflictType conflictType,
                boolean firstHasProperStudents, boolean secondHasProperStudents
        ) {
            if (firstRequirementId.equals(secondRequirementId)) {
                throw new IllegalArgumentException(
                        "Requirement " + firstRequirementId + " cannot conflict with itself.");
            }
            if (firstRequirementId < secondRequirementId) {
                return new ConflictPair(firstRequirementId, secondRequirementId, conflictType,
                        firstHasProperStudents, secondHasProperStudents);
            }
            return new ConflictPair(secondRequirementId, firstRequirementId, conflictType,
                    secondHasProperStudents, firstHasProperStudents);
        }
    }

    /** Same paper: every exam in the group starts in the same slot. */
    public record SharedExamGroup(List<Long> requirementIds) {}

    /** The exam must use exactly these venues; the solver chooses the day and session.*/
    public record FixedAssignment(Long requirementId, List<Long> venueIds) {}
}
