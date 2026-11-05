package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.dto.ExamSchedulingInput;
import com.university.exam_lecture_timetable.entity.CalendarPeriodEntity;
import com.university.exam_lecture_timetable.entity.ExamRequirementEntity;
import com.university.exam_lecture_timetable.entity.RequirementType;
import com.university.exam_lecture_timetable.entity.TimeslotEntity;
import com.university.exam_lecture_timetable.entity.VenueAvailabilityEntity;
import com.university.exam_lecture_timetable.entity.VenueEntity;
import com.university.exam_lecture_timetable.exception.ExamTimetableReviewException;
import com.university.exam_lecture_timetable.repository.CalendarPeriodRepository;
import com.university.exam_lecture_timetable.repository.ExamRequirementRepository;
import com.university.exam_lecture_timetable.repository.StudentGroupRepository;
import com.university.exam_lecture_timetable.repository.StudentGroupRepository.ModuleGroupMember;
import com.university.exam_lecture_timetable.repository.TimeslotRepository;
import com.university.exam_lecture_timetable.repository.VenueAvailabilityRepository;
import com.university.exam_lecture_timetable.repository.VenueRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamSchedulingInputService {

    private final CalendarPeriodRepository periodRepository;
    private final ExamRequirementRepository examRepository;
    private final StudentGroupRepository groupRepository;
    private final TimeslotRepository timeslotRepository;
    private final VenueRepository venueRepository;
    private final VenueAvailabilityRepository availabilityRepository;

    public ExamSchedulingInputService(
            CalendarPeriodRepository periodRepository,
            ExamRequirementRepository examRepository,
            StudentGroupRepository groupRepository,
            TimeslotRepository timeslotRepository,
            VenueRepository venueRepository,
            VenueAvailabilityRepository availabilityRepository
    ) {
        this.periodRepository = periodRepository;
        this.examRepository = examRepository;
        this.groupRepository = groupRepository;
        this.timeslotRepository = timeslotRepository;
        this.venueRepository = venueRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional(readOnly = true)
    public ExamSchedulingInput load(Long periodId) {
        return prepare(periodId, false).input();
    }

    @Transactional(readOnly = true)
    public DraftInput loadDraft(Long periodId) {
        return prepare(periodId, true);
    }

    private DraftInput prepare(Long periodId, boolean allowIncompleteMembership) {
        if (periodId == null || periodId <= 0) {
            throw new ExamTimetableReviewException("A positive exam period ID is required.");
        }
        CalendarPeriodEntity period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ExamTimetableReviewException("Unknown calendar period " + periodId + "."));
        List<ExamRequirementEntity> requirements =
                examRepository.findActiveByPeriodAndType(
                        periodId, RequirementType.EXAM);
        if (requirements.isEmpty()) {
            throw new ExamTimetableReviewException("No active exam requirements are assigned to period " + periodId + ".");
        }

        List<ExamWithStudents> exams = loadExams(requirements, allowIncompleteMembership);
        List<ExamSchedulingInput.Slot> slots = loadSlots(period);
        List<ExamSchedulingInput.Venue> venues = loadVenues(periodId, slots.size());
        var input = new ExamSchedulingInput(
                periodId, exams.stream().map(ExamWithStudents::exam).toList(),
                slots, venues, findStudentConflicts(exams));
        return new DraftInput(input, exams.stream().allMatch(ExamWithStudents::membershipComplete));
    }

    private List<ExamWithStudents> loadExams(
            List<ExamRequirementEntity> requirements, boolean allowIncompleteMembership
    ) {
        List<Long> moduleIds = requirements.stream()
                .map(exam -> exam.getRequirement().getModule().getModuleId())
                .distinct()
                .toList();
        Map<Long, Map<Long, GroupMembers>> groupsByModule = new HashMap<>();
        for (ModuleGroupMember row : groupRepository.findMembersByModuleIds(moduleIds)) {
            GroupMembers group = groupsByModule
                    .computeIfAbsent(row.getModuleId(), ignored -> new TreeMap<>())
                    .computeIfAbsent(row.getGroupId(), ignored -> new GroupMembers(row.getGroupSize(), new HashSet<>()));
            if (row.getStudentId() != null) {
                group.studentIds().add(row.getStudentId());
            }
        }
        return requirements.stream()
                .map(exam -> toExamInput(exam, groupsByModule, allowIncompleteMembership))
                .toList();
    }

    private ExamWithStudents toExamInput(
            ExamRequirementEntity exam, Map<Long, Map<Long, GroupMembers>> groupsByModule,
            boolean allowIncompleteMembership
    ) {
        Long requirementId = exam.getRequirementId();
        Integer duration = exam.getExamDurationMinutes();
        if (duration == null || duration <= 0) {
            throw new ExamTimetableReviewException("Exam requirement " + requirementId + " needs a positive duration.");
        }

        var module = exam.getRequirement().getModule();
        Map<Long, GroupMembers> groups = groupsByModule.get(module.getModuleId());
        if (groups == null || groups.isEmpty()) {
            throw new ExamTimetableReviewException("Module " + module.getModuleId() + " has no student groups.");
        }

        Set<Long> studentIds = new HashSet<>();
        List<ExamSchedulingInput.Group> examGroups = new ArrayList<>();
        int studentCount = 0;
        boolean membershipComplete = true;
        for (var entry : groups.entrySet()) {
            Long groupId = entry.getKey();
            GroupMembers group = entry.getValue();
            int memberCount = group.studentIds().size();
            Integer declaredSize = group.declaredSize();
            if (declaredSize == null || declaredSize <= 0 || memberCount > declaredSize
                    || (!allowIncompleteMembership && memberCount != declaredSize)) {
                throw new ExamTimetableReviewException(
                        "Student group " + groupId + " needs a complete, nonempty membership matching group_size.");
            }
            for (Long studentId : group.studentIds()) {
                if (!studentIds.add(studentId)) {
                    throw new ExamTimetableReviewException(
                            "Student groups for exam requirement " + requirementId
                                    + " share members. Assign each student to one group for this exam.");
                }
            }
            examGroups.add(new ExamSchedulingInput.Group(groupId, declaredSize));
            studentCount += declaredSize;
            membershipComplete &= memberCount == declaredSize;
        }

        var input = new ExamSchedulingInput.Exam(
                requirementId, module.getModuleId(), module.getModuleCode(), module.getModuleName(),
                module.getCurriculum(), duration, studentCount, List.copyOf(examGroups));
        Set<Long> groupIds = new HashSet<>(groups.keySet());
        return new ExamWithStudents(input, studentIds, groupIds, membershipComplete);
    }

    private List<ExamSchedulingInput.Slot> loadSlots(CalendarPeriodEntity period) {
        Long periodId = period.getPeriodId();
        var slots = timeslotRepository.findByPeriod_PeriodIdOrderByDateAscTimeslotAsc(periodId);
        if (slots.isEmpty()) {
            throw new ExamTimetableReviewException("No timeslots are defined for period " + periodId + ".");
        }

        for (TimeslotEntity slot : slots) {
            if (slot.getDate() == null || slot.getStartTime() == null || slot.getEndTime() == null
                    || !slot.getEndTime().isAfter(slot.getStartTime())) {
                throw new ExamTimetableReviewException("Timeslot " + slot.getTimeslotId() + " has invalid date or times.");
            }
            if ((period.getStartingDate() != null && slot.getDate().isBefore(period.getStartingDate()))
                    || (period.getEndDate() != null && slot.getDate().isAfter(period.getEndDate()))) {
                throw new ExamTimetableReviewException("Timeslot " + slot.getTimeslotId() + " is outside period " + periodId + ".");
            }
        }
        return slots.stream()
                .sorted(Comparator.comparing(TimeslotEntity::getDate)
                        .thenComparing(TimeslotEntity::getStartTime)
                        .thenComparing(TimeslotEntity::getTimeslotId))
                .map(slot -> new ExamSchedulingInput.Slot(
                        slot.getTimeslotId(), slot.getDate().toString(),
                        slot.getStartTime().toString(), slot.getEndTime().toString()))
                .toList();
    }

    private List<ExamSchedulingInput.Venue> loadVenues(Long periodId, int slotCount) {
        Map<Long, List<Long>> blockedSlots = new HashMap<>();
        for (VenueAvailabilityEntity row : availabilityRepository.findByTimeslot_Period_PeriodIdAndIsAvailableFalse(periodId)) {
            blockedSlots.computeIfAbsent(row.getId().getVenueId(), ignored -> new ArrayList<>())
                    .add(row.getId().getTimeslotId());
        }
        List<ExamSchedulingInput.Venue> venues = venueRepository.findByIsAvailableForExamsTrue().stream()
                .filter(venue -> venue.getExamCapacity() != null && venue.getExamCapacity() > 0)
                .sorted(Comparator.comparing(VenueEntity::getVenueId))
                .map(venue -> new ExamSchedulingInput.Venue(
                        venue.getVenueId(), venue.getVenueName(), venue.getExamCapacity(),
                        blockedSlots.getOrDefault(venue.getVenueId(), List.of()).stream().sorted().toList()))
                .filter(venue -> venue.unavailableTimeslotIds().size() < slotCount)
                .toList();
        if (venues.isEmpty()) {
            throw new ExamTimetableReviewException("No eligible exam venues are available in period " + periodId + ".");
        }
        return venues;
    }

    private List<ExamSchedulingInput.Conflict> findStudentConflicts(List<ExamWithStudents> exams) {
        List<ExamSchedulingInput.Conflict> conflicts = new ArrayList<>();
        for (int first = 0; first < exams.size(); first++) {
            ExamWithStudents left = exams.get(first);
            for (int second = first + 1; second < exams.size(); second++) {
                ExamWithStudents right = exams.get(second);
                if (!Collections.disjoint(left.studentIds(), right.studentIds())
                        || !Collections.disjoint(left.groupIds(), right.groupIds())) {
                    conflicts.add(new ExamSchedulingInput.Conflict(
                            left.exam().requirementId(), right.exam().requirementId()));
                }
            }
        }
        return List.copyOf(conflicts);
    }

    private record GroupMembers(Integer declaredSize, Set<Long> studentIds) {}

    public record DraftInput(ExamSchedulingInput input, boolean membershipComplete) {}

    private record ExamWithStudents(
            ExamSchedulingInput.Exam exam, Set<Long> studentIds, Set<Long> groupIds, boolean membershipComplete
    ) {}
}
