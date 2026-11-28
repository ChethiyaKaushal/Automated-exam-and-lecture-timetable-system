package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.dto.solver.SolverInput.ConflictPair;
import com.university.exam_lecture_timetable.dto.solver.SolverInput.ConflictType;
import com.university.exam_lecture_timetable.entity.EnrollmentType;
import com.university.exam_lecture_timetable.entity.RegistrationStatus;
import com.university.exam_lecture_timetable.repository.StudentModuleEnrollmentRepository;
import com.university.exam_lecture_timetable.repository.StudentModuleEnrollmentRepository.EnrollmentRow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ExamConflictService {

    private final StudentModuleEnrollmentRepository enrollmentRepository;

    public ExamConflictService(StudentModuleEnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * @param requirementIdsByModule moduleId -> exam requirement IDs being scheduled for that module
     * @param semesterIds            semesters of the exam period; empty means "do not filter by semester"
     */
    @Transactional(readOnly = true)
    public List<ConflictPair> findConflictPairs(
            Map<Long, List<Long>> requirementIdsByModule, Set<Long> semesterIds
    ) {
        if (requirementIdsByModule.size() < 2) {
            return List.of();
        }
        List<EnrollmentRow> rows = enrollmentRepository.findEnrollmentsForModules(
                requirementIdsByModule.keySet(), RegistrationStatus.CONFIRMED);
        List<EnrollmentRow> inScope = semesterIds.isEmpty()
                ? rows
                : rows.stream().filter(row -> semesterIds.contains(row.getSemesterId())).toList();
        return buildPairs(requirementIdsByModule, inScope);
    }

    /** Pure logic, separated from the database so it can be unit tested. */
    static List<ConflictPair> buildPairs(
            Map<Long, List<Long>> requirementIdsByModule, Collection<EnrollmentRow> rows
    ) {
        // studentId -> (moduleId -> is PROPER). A PROPER row wins if a student has both.
        Map<Long, Map<Long, Boolean>> modulesByStudent = new HashMap<>();
        for (EnrollmentRow row : rows) {
            if (!requirementIdsByModule.containsKey(row.getModuleId())) {
                continue;
            }
            boolean proper = row.getEnrollmentType() == EnrollmentType.PROPER;
            modulesByStudent
                    .computeIfAbsent(row.getStudentId(), ignored -> new TreeMap<>())
                    .merge(row.getModuleId(), proper, Boolean::logicalOr);
        }

        // (smaller moduleId, larger moduleId) -> what the common students look like
        Map<ModulePair, PairFacts> facts = new TreeMap<>();
        for (Map<Long, Boolean> modules : modulesByStudent.values()) {
            List<Map.Entry<Long, Boolean>> entries = new ArrayList<>(modules.entrySet());
            for (int i = 0; i < entries.size(); i++) {
                for (int j = i + 1; j < entries.size(); j++) {
                    var low = entries.get(i);
                    var high = entries.get(j);
                    PairFacts pair = facts.computeIfAbsent(
                            new ModulePair(low.getKey(), high.getKey()), ignored -> new PairFacts());
                    pair.lowHasProper |= low.getValue();
                    pair.highHasProper |= high.getValue();
                    pair.bothProper |= low.getValue() && high.getValue();

                    //`|=` means **"OR-equals"**. Once it becomes `true`, it will always remain `true`.
                }
            }
        }

        // Expand module pairs to requirement pairs and put every pair in canonical order.
        Map<RequirementPair, ConflictPair> pairs = new HashMap<>();
        for (var entry : facts.entrySet()) {
            ModulePair modules = entry.getKey();
            PairFacts pair = entry.getValue();
            ConflictType type = pair.bothProper ? ConflictType.DIFFERENT_DAY : ConflictType.NO_TIME_OVERLAP;
            for (Long lowRequirement : requirementIdsByModule.get(modules.low())) {
                for (Long highRequirement : requirementIdsByModule.get(modules.high())) {
                    if (lowRequirement.equals(highRequirement)) {
                        continue;
                    }
                    ConflictPair canonical = ConflictPair.canonical(
                            lowRequirement, highRequirement, type, pair.lowHasProper, pair.highHasProper);
                    pairs.merge(
                            new RequirementPair(canonical.firstRequirementId(), canonical.secondRequirementId()),
                            canonical, ExamConflictService::stricter);
                }
            }
        }
        return pairs.values().stream()
                .sorted(Comparator.comparing(ConflictPair::firstRequirementId)
                        .thenComparing(ConflictPair::secondRequirementId))
                .toList();
    }

    /** If the same requirement pair is reached twice, keep the stricter rule and OR the flags. */
    private static ConflictPair stricter(ConflictPair a, ConflictPair b) {
        ConflictType type = a.conflictType() == ConflictType.DIFFERENT_DAY || b.conflictType() == ConflictType.DIFFERENT_DAY
                ? ConflictType.DIFFERENT_DAY
                : ConflictType.NO_TIME_OVERLAP;
        return new ConflictPair(a.firstRequirementId(), a.secondRequirementId(), type,
                a.firstHasProperStudents() || b.firstHasProperStudents(),
                a.secondHasProperStudents() || b.secondHasProperStudents());
    }

    private record ModulePair(Long low, Long high) implements Comparable<ModulePair> {
        @Override
        public int compareTo(ModulePair other) {
            int byLow = low.compareTo(other.low);
            return byLow != 0 ? byLow : high.compareTo(other.high);
        }
    }

    private record RequirementPair(Long first, Long second) {}

    private static final class PairFacts {
        boolean lowHasProper;
        boolean highHasProper;
        boolean bothProper;
    }
}
