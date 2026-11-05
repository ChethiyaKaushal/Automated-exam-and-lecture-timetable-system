package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.Mapper.ExamTimetableMapper;
import com.university.exam_lecture_timetable.dto.request.StartAssignment;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.DraftOption;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.ExamOption;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.PeriodOption;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Preview;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Review;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Saved;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Setup;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.SlotOption;
import com.university.exam_lecture_timetable.validation.ExamTimetableReviewValidator;
import com.university.exam_lecture_timetable.entity.AppUserEntity;
import com.university.exam_lecture_timetable.entity.AuditLogEntity;
import com.university.exam_lecture_timetable.entity.CalendarPeriodEntity;
import com.university.exam_lecture_timetable.entity.EventRequirementEntity;
import com.university.exam_lecture_timetable.entity.EventSlotEntity;
import com.university.exam_lecture_timetable.entity.ExamRequirementEntity;
import com.university.exam_lecture_timetable.entity.ModuleEntity;
import com.university.exam_lecture_timetable.entity.RequirementType;
import com.university.exam_lecture_timetable.entity.TimeslotEntity;
import com.university.exam_lecture_timetable.entity.TimetableEntity;
import com.university.exam_lecture_timetable.entity.TimetableStatus;
import com.university.exam_lecture_timetable.exception.ExamTimetableReviewException;
import com.university.exam_lecture_timetable.repository.AppUserRepository;
import com.university.exam_lecture_timetable.repository.CalendarPeriodRepository;
import com.university.exam_lecture_timetable.repository.EventRepository;
import com.university.exam_lecture_timetable.repository.ModuleRepository;
import com.university.exam_lecture_timetable.repository.TimeslotRepository;
import com.university.exam_lecture_timetable.repository.TimetableRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminExamTimetableService {

    private final EntityManager em;
    private final CalendarPeriodRepository periods;
    private final ModuleRepository modules;
    private final TimeslotRepository slots;
    private final TimetableRepository timetables;
    private final EventRepository events;
    private final AppUserRepository users;
    private final ExamTimetableDraftService generator;
    private final GeminiExamTimetableService gemini;
    private final ExamTimetableReviewValidator reviews;

    public AdminExamTimetableService(
            EntityManager em, CalendarPeriodRepository periods, ModuleRepository modules, TimeslotRepository slots,
            TimetableRepository timetables, EventRepository events, AppUserRepository users,
            ExamTimetableDraftService generator, GeminiExamTimetableService gemini, ExamTimetableReviewValidator reviews
    ) {
        this.em = em;
        this.periods = periods;
        this.modules = modules;
        this.slots = slots;
        this.timetables = timetables;
        this.events = events;
        this.users = users;
        this.generator = generator;
        this.gemini = gemini;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public List<PeriodOption> periods() {
        return periods.findAll().stream().filter(p -> "EXAM".equalsIgnoreCase(p.getPeriodType()))
                .sorted(Comparator.comparing(CalendarPeriodEntity::getPeriodId).reversed())
                .map(AdminExamTimetableService::option).toList();
    }

    @Transactional(readOnly = true)
    public Setup setup(long periodId) {
        var period = requirePeriod(periodId, false);
        var configurations = em.createQuery("""
                select r from EventRequirementEntity r join fetch r.module left join fetch r.examRequirement
                where r.period.periodId = :periodId and r.requirementType = :type
                """, EventRequirementEntity.class)
                .setParameter("periodId", periodId).setParameter("type", RequirementType.EXAM).getResultList();
        Map<Long, EventRequirementEntity> byModule = new HashMap<>();
        for (var config : configurations) {
            if (byModule.putIfAbsent(config.getModule().getModuleId(), config) != null) {
                throw new ExamTimetableReviewException("An exam module has duplicate requirements in this period.");
            }
        }
        List<ExamOption> options = new ArrayList<>();
        for (var semester : period.getSemesters()) {
            for (var module : modules.findBySemester_SemesterId(semester.getSemesterId())) {
                var requirement = byModule.get(module.getModuleId());
                var exam = requirement == null ? null : requirement.getExamRequirement();
                options.add(new ExamOption(module.getModuleId(), module.getModuleCode(), module.getModuleName(),
                        module.getCurriculum(), semester.getSemesterName(), exam != null && Boolean.TRUE.equals(exam.getHasExam()),
                        exam == null ? 120 : exam.getExamDurationMinutes()));
            }
        }
        options.sort(Comparator.comparing(ExamOption::semester).thenComparing(ExamOption::moduleCode)
                .thenComparing(ExamOption::moduleName).thenComparing(ExamOption::curriculum));
        var slotOptions = slots.findByPeriod_PeriodIdOrderByDateAscTimeslotAsc(periodId).stream()
                .filter(slot -> slot.getDate() != null && slot.getStartTime() != null && slot.getEndTime() != null)
                .sorted(Comparator.comparing(TimeslotEntity::getDate).thenComparing(TimeslotEntity::getStartTime))
                .map(slot -> new SlotOption(slot.getTimeslotId(), slot.getDate().toString(), slot.getStartTime().toString()))
                .toList();
        var drafts = timetables.findByPeriod_PeriodIdAndDepartment_DepartmentId(periodId, null).stream()
                .filter(t -> t.getTimetableType() == RequirementType.EXAM && t.getStatus() == TimetableStatus.DRAFT)
                .sorted(Comparator.comparing(TimetableEntity::getVersionNum).reversed())
                .map(t -> new DraftOption(t.getTimetableId(), t.getVersionNum())).toList();
        return new Setup(option(period), List.copyOf(options), slotOptions, drafts);
    }

    @Transactional
    public void configure(long periodId, long moduleId, boolean enabled, int duration) {
        var period = requirePeriod(periodId, true);
        var module = modules.findById(moduleId).orElseThrow(() -> new ExamTimetableReviewException("Module not found."));
        if (period.getSemesters().stream().noneMatch(s -> s.getSemesterId().equals(module.getSemester().getSemesterId()))) {
            throw new ExamTimetableReviewException("That module's semester is not included in this exam period.");
        }
        if (duration != 120 && duration != 180) {
            throw new ExamTimetableReviewException("Select a two-hour or a three-hour exam duration.");
        }
        if ("ME1201".equals(module.getModuleCode()) && "C-23".equals(module.getCurriculum())
                && (!enabled || duration != 180)) {
            throw new ExamTimetableReviewException("Drawing C-23 uses four fixed three-hour groups.");
        }

        var matches = em.createQuery("""
                select r from EventRequirementEntity r
                where r.period = :period and r.module = :module and r.requirementType = :type
                """, EventRequirementEntity.class).setParameter("period", period)
                .setParameter("module", module).setParameter("type", RequirementType.EXAM).getResultList();
        if (matches.size() > 1) {
            throw new ExamTimetableReviewException("That module has duplicate requirements in this period.");
        }
        if (matches.isEmpty() && !enabled) {
            return;
        }
        EventRequirementEntity requirement;
        if (matches.isEmpty()) {
            requirement = new EventRequirementEntity();
            requirement.setPeriod(period);
            requirement.setModule(module);
            requirement.setRequirementType(RequirementType.EXAM);
            em.persist(requirement);
        } else {
            requirement = matches.getFirst();
        }
        var exam = requirement.getExamRequirement();
        if (exam == null) {
            exam = new ExamRequirementEntity();
            exam.setRequirement(requirement);
            requirement.setExamRequirement(exam);
            exam.setHasExam(enabled);
            exam.setExamDurationMinutes(duration);
            em.persist(exam);
        } else {
            exam.setHasExam(enabled);
            exam.setExamDurationMinutes(duration);
        }
    }

    public Preview preview(long periodId) {
        return preview(periodId, "java");
    }

    public Preview preview(long periodId, String engine) {
        var draft = switch (engine) {
            case "java" -> generator.generate(periodId);
            case "gemini" -> gemini.generate(periodId);
            default -> throw new ExamTimetableReviewException("Choose java or gemini as the draft engine.");
        };
        var assignments = ExamTimetableMapper.starts(draft, "gemini".equals(engine));
        return new Preview(assignments, reviews.review(periodId, assignments));
    }

    public Review review(long periodId, List<StartAssignment> assignments) {
        return reviews.review(periodId, assignments);
    }

    @Transactional(readOnly = true)
    public Preview open(long periodId, long timetableId) {
        var timetable = timetables.findById(timetableId)
                .filter(t -> t.getPeriod().getPeriodId().equals(periodId))
                .filter(t -> t.getDepartment() == null && t.getTimetableType() == RequirementType.EXAM
                        && t.getStatus() == TimetableStatus.DRAFT)
                .orElseThrow(() -> new ExamTimetableReviewException("Draft not found for this exam period."));
        var assignments = timetable.getEvents().stream().map(event -> {
            var first = event.getEventSlots().stream().map(EventSlotEntity::getTimeslot)
                    .min(Comparator.comparing(TimeslotEntity::getDate).thenComparing(TimeslotEntity::getStartTime));
            var venueIds = event.getAllocations().stream().map(allocation -> allocation.getVenue().getVenueId())
                    .sorted().toList();
            return new StartAssignment(event.getRequirement().getRequirementId(),
                    first.map(TimeslotEntity::getTimeslotId).orElse(null), venueIds.isEmpty() ? null : venueIds);
        }).toList();
        return new Preview(assignments, reviews.review(periodId, assignments));
    }

    @Transactional
    public Saved save(long periodId, List<StartAssignment> assignments, boolean manualReview, String username) {
        var period = requirePeriod(periodId, true);
        var review = reviews.review(periodId, assignments);
        if (!review.valid() || (!review.studentMembershipsComplete() && !manualReview)) {
            return new Saved(null, null, review, !review.studentMembershipsComplete() && !manualReview);
        }

        AppUserEntity admin = users.findByUsername(username)
                .orElseThrow(() -> new ExamTimetableReviewException("The authenticated admin account no longer exists."));
        int version = timetables.findFirstByPeriod_PeriodIdAndDepartment_DepartmentIdOrderByVersionNumDesc(periodId, null)
                .map(t -> t.getVersionNum() + 1).orElse(1);
        var timetable = new TimetableEntity();
        timetable.setPeriod(period);
        timetable.setVersionNum(version);
        timetable.setTimetableType(RequirementType.EXAM);
        timetable.setStatus(TimetableStatus.DRAFT);
        timetable.setCreatedByUser(admin);
        timetables.save(timetable);
        for (var scheduled : review.exams()) {
            events.save(ExamTimetableMapper.toEvent(em, timetable, scheduled));
        }
        var audit = new AuditLogEntity();
        audit.setAction("EXAM_DRAFT_SAVED");
        audit.setEntryType("TIMETABLE");
        audit.setEntityId(timetable.getTimetableId().toString());
        audit.setUser(admin);
        audit.setDetails("Version " + version + "; student memberships complete: "
                + review.studentMembershipsComplete() + "; manual review acknowledged: " + manualReview);
        em.persist(audit);
        return new Saved(timetable.getTimetableId(), version, review, false);
    }

    private CalendarPeriodEntity requirePeriod(long id, boolean lock) {
        var period = lock ? em.find(CalendarPeriodEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(CalendarPeriodEntity.class, id);
        if (period == null || !"EXAM".equalsIgnoreCase(period.getPeriodType())) {
            throw new ExamTimetableReviewException("Select a valid exam period.");
        }
        return period;
    }

    private static PeriodOption option(CalendarPeriodEntity period) {
        return new PeriodOption(period.getPeriodId(), period.getPeriodName(), period.getStartingDate(), period.getEndDate());
    }
}
