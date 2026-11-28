package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.dto.ExamSchedulingInput;
import com.university.exam_lecture_timetable.dto.solver.SolverInput;
import com.university.exam_lecture_timetable.entity.*;
import com.university.exam_lecture_timetable.exception.ExamTimetableReviewException;
import com.university.exam_lecture_timetable.repository.*;
import com.university.exam_lecture_timetable.repository.StudentGroupRepository.ModuleGroupMember;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamSchedulingInputService {

    private final CalendarPeriodRepository periodRepository;
    private final ExamRequirementRepository examRepository;
    private final StudentGroupRepository groupRepository;
    private final TimeslotRepository timeslotRepository;
    private final VenueRepository venueRepository;
    private final VenueAvailabilityRepository availabilityRepository;
    private final EventAllocationRepository allocationRepository;
    private final ExamConflictService conflictService;


    public ExamSchedulingInputService(
            CalendarPeriodRepository periodRepository,
            ExamRequirementRepository examRepository,
            StudentGroupRepository groupRepository,
            TimeslotRepository timeslotRepository,
            VenueRepository venueRepository,
            VenueAvailabilityRepository availabilityRepository,
            EventAllocationRepository allocationRepository,
            ExamConflictService conflictService
    ) {
        this.periodRepository = periodRepository;
        this.examRepository = examRepository;
        this.groupRepository = groupRepository;
        this.timeslotRepository = timeslotRepository;
        this.venueRepository = venueRepository;
        this.availabilityRepository = availabilityRepository;
        this.allocationRepository = allocationRepository;
        this.conflictService = conflictService;
    }

    @Transactional(readOnly = true)
    public ExamSchedulingInput load(Long periodId) {
        return prepare(periodId, false).input();
    }

    @Transactional(readOnly = true)
    public DraftInput loadDraft(Long periodId) {
        return prepare(periodId, true);
    }

    /**
     * The solver contract links requiredVenueType and fixedAssignments: an exam with a required type
     * must list the venues it may use, all of that type. There is no table of chosen venues per exam
     * yet, so every eligible venue of the required type is allowed and the solver picks among them.
     */
    static List<SolverInput.FixedAssignment> buildFixedAssignments(
            List<SolverInput.Exam> exams, List<SolverInput.Venue> venues
    ) {
        List<SolverInput.FixedAssignment> result = new ArrayList<>();
        for (SolverInput.Exam exam : exams) {
            if (exam.requiredVenueType() == null) {
                continue;
            }
            List<Long> venueIds = venues.stream()
                    .filter(venue -> venue.venueType() == exam.requiredVenueType())
                    .map(SolverInput.Venue::venueId)
                    .toList();
            if (venueIds.isEmpty()) {
                throw new ExamTimetableReviewException(exam.moduleCode() + " (" + exam.curriculum() + ") needs a "
                        + exam.requiredVenueType() + " venue, but none is available for exams.");
            }
            result.add(new SolverInput.FixedAssignment(exam.requirementId(), venueIds));
        }
        return List.copyOf(result);
    }

    private static SolverInput.Exam toSolverExam(ExamRequirementEntity requirement, ExamSchedulingInput.Exam exam) {
        var module = requirement.getRequirement().getModule();
        Integer gapDays = requirement.getExamGapDays();
        if (gapDays != null && gapDays < 0) {
            throw new ExamTimetableReviewException(
                    "Exam requirement " + exam.requirementId() + " has a negative exam gap.");
        }
        return new SolverInput.Exam(
                exam.requirementId(), exam.moduleId(), exam.moduleCode(), exam.moduleName(), exam.curriculum(),
                module.getDepartment() == null ? null : module.getDepartment().getDepartmentId(),
                module.getSemester() == null ? null : module.getSemester().getSemesterId(),
                exam.durationMinutes(),
                gapDays == null ? 0 : gapDays,
                requirement.getRequiredVenueType(),
                exam.studentCount(),
                exam.groups().stream().map(group -> new SolverInput.Group(group.groupId(), group.studentCount())).toList());
    }

    private CalendarPeriodEntity findPeriod(Long periodId) {
        if (periodId == null || periodId <= 0) {
            throw new ExamTimetableReviewException("A positive exam period ID is required.");
        }
        return periodRepository.findById(periodId)
                .orElseThrow(() -> new ExamTimetableReviewException("Unknown calendar period " + periodId + "."));
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


    //=================================New method - mapper========================================

    private List<ExamRequirementEntity> findRequirements(Long periodId) {
        List<ExamRequirementEntity> requirements =
                examRepository.findActiveByPeriodAndType(
                        periodId, RequirementType.EXAM);
        if (requirements.isEmpty()) {
            throw new ExamTimetableReviewException("No active exam requirements are assigned to period " + periodId + ".");
        }
        return requirements;
    }

    private DraftInput prepare(Long periodId, boolean allowIncompleteMembership) {
        CalendarPeriodEntity period = findPeriod(periodId);
        List<ExamRequirementEntity> requirements = findRequirements(periodId);

        List<ExamWithStudents> exams = loadExams(requirements, allowIncompleteMembership);
        List<ExamSchedulingInput.Slot> slots = loadSlots(period);
        List<ExamSchedulingInput.Venue> venues = loadVenues(periodId, slots.size());
        var input = new ExamSchedulingInput(
                periodId, exams.stream().map(ExamWithStudents::exam).toList(),
                slots, venues, findStudentConflicts(exams));
        return new DraftInput(input, exams.stream().allMatch(ExamWithStudents::membershipComplete));
    }

    /**
     * Builds the request for the Python CP-SAT solver (ai-service/app/schemas/solver_input.py).
     *
     * <ul>
     *   <li>Exams with hasExam = false are left out (filtered in the repository query).</li>
     *   <li>Venues with isAvailableForExams = false are left out completely.</li>
     *   <li>unavailableTimeslotIds = slots marked unavailable + slots already booked by other saved events.</li>
     *   <li>conflictPairs come from student_module_enrollment (see ExamConflictService).</li>
     *   <li>sharedExamGroups: sent empty for now (shared exam pairs are not stored yet).</li>
     *   <li>fixedAssignments: every exam with a requiredVenueType gets all venues of that type.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public SolverInput loadSolverInput(Long periodId) {
        CalendarPeriodEntity period = findPeriod(periodId);
        List<ExamRequirementEntity> requirements = findRequirements(periodId);

        // Group sizes are what the solver needs; group membership only fed the old clash check.
        List<ExamWithStudents> loaded = loadExams(requirements, true);
        List<SolverInput.Exam> exams = new ArrayList<>();
        Map<Long, List<Long>> requirementIdsByModule = new TreeMap<>();
        for (int i = 0; i < requirements.size(); i++) {
            exams.add(toSolverExam(requirements.get(i), loaded.get(i).exam()));
            requirementIdsByModule
                    .computeIfAbsent(exams.get(i).moduleId(), ignored -> new ArrayList<>())
                    .add(exams.get(i).requirementId());
        }
        Set<Long> requirementIds = exams.stream().map(SolverInput.Exam::requirementId).collect(Collectors.toSet());

        List<SolverInput.Slot> slots = loadSlots(period).stream()
                .map(slot -> new SolverInput.Slot(slot.timeslotId(), slot.date(), slot.startTime(), slot.endTime()))
                .toList();
        List<SolverInput.Venue> venues = loadSolverVenues(periodId, slots.size(), requirementIds);

        Set<Long> semesterIds = period.getSemesters().stream()
                .map(SemesterEntity::getSemesterId)
                .collect(Collectors.toSet());
        List<SolverInput.ConflictPair> conflictPairs =
                conflictService.findConflictPairs(requirementIdsByModule, semesterIds);

        // Shared exams are not stored yet, so none are sent for now.
        List<SolverInput.SharedExamGroup> sharedGroups = List.of();

        return new SolverInput(periodId, List.copyOf(exams), slots, venues,
                conflictPairs, sharedGroups, buildFixedAssignments(exams, venues));
    }

    private List<SolverInput.Venue> loadSolverVenues(Long periodId, int slotCount, Set<Long> scheduledRequirementIds) {
        Map<Long, Set<Long>> blocked = new HashMap<>();
        for (VenueAvailabilityEntity row : availabilityRepository.findByTimeslot_Period_PeriodIdAndIsAvailableFalse(periodId)) {
            blocked.computeIfAbsent(row.getId().getVenueId(), ignored -> new TreeSet<>())
                    .add(row.getId().getTimeslotId());
        }
        for (var booked : allocationRepository.findBookedSlots(periodId, scheduledRequirementIds, TimetableStatus.ARCHIVED)) {
            blocked.computeIfAbsent(booked.getVenueId(), ignored -> new TreeSet<>())
                    .add(booked.getTimeslotId());
        }
        List<SolverInput.Venue> venues = venueRepository.findByIsAvailableForExamsTrue().stream()
                .filter(venue -> venue.getExamCapacity() != null && venue.getExamCapacity() > 0)
                .sorted(Comparator.comparing(VenueEntity::getVenueId))
                .map(venue -> new SolverInput.Venue(
                        venue.getVenueId(), venue.getVenueName(), venue.getVenueType(), venue.getExamCapacity(),
                        venue.getPriority() == null ? 1 : venue.getPriority(),
                        List.copyOf(blocked.getOrDefault(venue.getVenueId(), Set.of()))))
                // A venue that is blocked in every slot can never be used.
                .filter(venue -> venue.unavailableTimeslotIds().size() < slotCount)
                .toList();
        if (venues.isEmpty()) {
            throw new ExamTimetableReviewException("No eligible exam venues are available in period " + periodId + ".");
        }
        return venues;
    }
}
