package com.university.exam_lecture_timetable;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.university.exam_lecture_timetable.entity.*;
import com.university.exam_lecture_timetable.service.AdminExamTimetableService;
import com.university.exam_lecture_timetable.dto.request.StartAssignment;
import com.university.exam_lecture_timetable.dto.request.DraftRequest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TIMETABLE_ISOLATED_DB_TEST", matches = "true")
class AdminExamTimetableIntegrationTests {

    @Autowired AdminExamTimetableService service;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate sql;
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;

    @Test
    void adminEditsAndSavesOnlyValidAcknowledgedDrafts() throws Exception {
        assertEquals("timetable_admin_test", sql.queryForObject("select current_database()", String.class));
        var ids = tx.execute(status -> fixture());
        long period = ids.get(0);
        long firstModule = ids.get(1);
        long secondModule = ids.get(2);
        String api = "/api/admin/exam-timetables/" + period;

        mvc.perform(get(api)).andExpect(status().is3xxRedirection());
        mvc.perform(get(api).with(user("student").roles("STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(get(api).with(user("fixture-admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(put(api + "/exams/" + firstModule).with(user("fixture-admin").roles("ADMIN"))
                .contentType("application/json").content("{\"enabled\":true,\"durationMinutes\":120}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(api + "/exams/" + firstModule).with(user("fixture-admin").roles("ADMIN")).with(csrf())
                .contentType("application/json").content("{\"enabled\":true,\"durationMinutes\":120}"))
                .andExpect(status().isNoContent());
        service.configure(period, secondModule, true, 120);

        var initial = service.preview(period);
        assertTrue(initial.review().valid());
        assertFalse(initial.review().studentMembershipsComplete());
        assertEquals(2, initial.assignments().size());
        var a = initial.assignments().getFirst();
        var b = initial.assignments().getLast();
        var clash = List.of(a, new StartAssignment(b.requirementId(), a.startTimeslotId()));
        var invalid = service.save(period, clash, true, "fixture-admin");
        assertNull(invalid.timetableId());
        assertTrue(invalid.review().issues().stream().anyMatch(i -> "VENUES_UNAVAILABLE".equals(i.code())));

        var swapped = List.of(new StartAssignment(a.requirementId(), b.startTimeslotId()),
                new StartAssignment(b.requirementId(), a.startTimeslotId()));
        assertTrue(service.review(period, swapped).valid());
        String body = json.writeValueAsString(new DraftRequest(swapped, false));
        mvc.perform(post(api + "/drafts").with(user("fixture-admin").roles("ADMIN")).with(csrf())
                .contentType("application/json").content(body))
                .andExpect(status().is(422)).andExpect(jsonPath("$.manualReviewRequired").value(true));
        assertEquals(0, sql.queryForObject("select count(*) from timetable where period_id = ?", Integer.class, period));

        var saved = service.save(period, swapped, true, "fixture-admin");
        assertNotNull(saved.timetableId());
        assertEquals(1, saved.version());
        assertEquals("DRAFT", sql.queryForObject("select status from timetable where timetable_id = ?",
                String.class, saved.timetableId()));
        assertEquals(2, sql.queryForObject("select count(*) from event where timetable_id = ?", Integer.class, saved.timetableId()));
        assertEquals(8, sql.queryForObject("select count(*) from event_slot s join event e using(event_id) where e.timetable_id = ?",
                Integer.class, saved.timetableId()));
        assertEquals(2, sql.queryForObject("select count(*) from event_allocation a join event e using(event_id) where e.timetable_id = ?",
                Integer.class, saved.timetableId()));
        assertEquals(80, sql.queryForObject("""
                select sum(g.allocated_count) from student_group_allocation g
                join event_allocation a using(allocation_id) join event e using(event_id) where e.timetable_id = ?
                """, Integer.class, saved.timetableId()));
        assertEquals(1, sql.queryForObject("select count(*) from audit_log where entity_id = ? and action = 'EXAM_DRAFT_SAVED'",
                Integer.class, saved.timetableId().toString()));
        assertTrue(service.open(period, saved.timetableId()).assignments().containsAll(swapped));
        assertEquals(2, service.save(period, swapped, true, "fixture-admin").version());
    }

    private List<Long> fixture() {
        var admin = new AppUserEntity();
        admin.setUsername("fixture-admin");
        admin.setEmail("fixture-admin@example.invalid");
        admin.setPasswordHash("unused-in-this-test");
        em.persist(admin);

        LocalDate day = LocalDate.of(2026, 12, 1);
        var department = new DepartmentEntity();
        department.setDepartmentName("Test Department");
        em.persist(department);
        var semester = new SemesterEntity();
        semester.setAcademicYear("2025/2026");
        semester.setSemesterName("1");
        semester.setStartingDate(day);
        em.persist(semester);
        var period = new CalendarPeriodEntity();
        period.setPeriodName("Isolated test exam period");
        period.setPeriodType("EXAM");
        period.setStartingDate(day);
        period.setEndDate(day);
        period.getSemesters().add(semester);
        em.persist(period);

        for (LocalTime start : List.of(LocalTime.of(9, 0), LocalTime.of(13, 30))) {
            for (int i = 0; i < 4; i++) {
                LocalTime slotStart = start.plusMinutes(30L * i);
                var slot = new TimeslotEntity();
                slot.setDate(day);
                slot.setPeriod(period);
                slot.setTimeslot(slotStart.toString());
                slot.setStartTime(slotStart);
                slot.setEndTime(slotStart.plusMinutes(30));
                em.persist(slot);
            }
        }
        var venue = new VenueEntity();
        venue.setVenueName("Test hall");
        venue.setVenueType("Lecture Hall");
        venue.setExamCapacity(40);
        venue.setLectureCapacity(50);
        venue.setPriority(1);
        em.persist(venue);
        return List.of(period.getPeriodId(), module("AA1201", department, semester), module("AA1202", department, semester));
    }

    private long module(String code, DepartmentEntity department, SemesterEntity semester) {
        var module = new ModuleEntity();
        module.setModuleCode(code);
        module.setModuleName(code);
        module.setCurriculum("C-23");
        module.setCredit(2);
        module.setDepartment(department);
        module.setSemester(semester);
        em.persist(module);
        var group = new StudentGroupEntity();
        group.setSemester(semester);
        group.setGroupSize(40);
        group.getModules().add(module);
        em.persist(group);
        return module.getModuleId();
    }
}
