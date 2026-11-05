package com.university.exam_lecture_timetable;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.university.exam_lecture_timetable.dto.request.CreateSetupRequestDto;
import com.university.exam_lecture_timetable.dto.request.DraftRequest;
import com.university.exam_lecture_timetable.dto.request.SaveRequirementsRequest;
import com.university.exam_lecture_timetable.dto.request.SaveRequirementsRequest.ModuleRequirement;
import com.university.exam_lecture_timetable.dto.request.StartAssignment;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Preview;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableResponses.Saved;
import com.university.exam_lecture_timetable.dto.response.SetupResponse;
import com.university.exam_lecture_timetable.entity.*;
import com.university.exam_lecture_timetable.service.AdminExamTimetableService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired PasswordEncoder passwords;

    private static final String ADMIN_EMAIL = "fixture-admin@example.invalid";
    private static final String STUDENT_EMAIL = "fixture-student@example.invalid";
    private static final String PASSWORD = "fixture-password";

    @Test
    void adminCreatesSlotsAndSavesOnlyValidAcknowledgedDrafts() throws Exception {
        assertEquals("timetable_admin_test", sql.queryForObject("select current_database()", String.class));
        var ids = tx.execute(status -> fixture());
        var admin = login(ADMIN_EMAIL);
        var student = login(STUDENT_EMAIL);
        long adminId = ids.get(3);
        long period = createExamPeriod(ids.get(0), admin, student);
        long firstModule = ids.get(1);
        long secondModule = ids.get(2);
        String api = "/api/admin/exam-timetables/" + period;

        String jwtResponse = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json.writeValueAsString(java.util.Map.of("email", ADMIN_EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.email").value(ADMIN_EMAIL))
                .andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + json.readTree(jwtResponse).path("data").path("token").asString();
        mvc.perform(get("/api/users").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/users").param("search", "FIXTURE-STUDENT").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].email").value(STUDENT_EMAIL));
        mvc.perform(get(api).header("Authorization", bearer)).andExpect(status().is3xxRedirection());

        mvc.perform(get(api)).andExpect(status().is3xxRedirection());
        mvc.perform(get(api).session(student)).andExpect(status().isForbidden());
        mvc.perform(get(api).session(admin)).andExpect(status().isOk());
        mvc.perform(put(api + "/exams/" + firstModule).session(admin)
                .contentType("application/json").content("{\"enabled\":true,\"durationMinutes\":120}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(api + "/exams/" + firstModule).session(admin).with(csrf())
                .contentType("application/json").content("{\"enabled\":true,\"durationMinutes\":120}"))
                .andExpect(status().isNoContent());
        String requirements = json.writeValueAsString(new SaveRequirementsRequest(List.of(period),
                List.of(new ModuleRequirement(secondModule, true, 180))));
        mvc.perform(post("/api/admin/timetable-setup/requirements").session(student).with(csrf())
                .contentType("application/json").content(requirements)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/timetable-setup/requirements").session(admin).with(csrf())
                .contentType("application/json").content(requirements)).andExpect(status().isNoContent());

        String previewBody = mvc.perform(post(api + "/preview").session(admin).with(csrf()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var initial = json.readValue(previewBody, Preview.class);
        assertEquals(List.of(120, 180),
                initial.review().exams().stream().map(exam -> exam.durationMinutes()).sorted().toList());
        assertTrue(initial.review().valid());
        assertFalse(initial.review().studentMembershipsComplete());
        assertEquals(2, initial.assignments().size());
        var a = initial.assignments().getFirst();
        var b = initial.assignments().getLast();
        var clash = List.of(a, new StartAssignment(b.requirementId(), a.startTimeslotId()));
        var invalid = service.save(period, clash, true, adminId);
        assertNull(invalid.timetableId());
        assertTrue(invalid.review().issues().stream().anyMatch(i -> "VENUES_UNAVAILABLE".equals(i.code())));

        var swapped = List.of(new StartAssignment(a.requirementId(), b.startTimeslotId()),
                new StartAssignment(b.requirementId(), a.startTimeslotId()));
        assertTrue(service.review(period, swapped).valid());
        String body = json.writeValueAsString(new DraftRequest(swapped, false));
        mvc.perform(post(api + "/drafts").session(admin).with(csrf())
                .contentType("application/json").content(body))
                .andExpect(status().is(422)).andExpect(jsonPath("$.manualReviewRequired").value(true));
        assertEquals(0, sql.queryForObject("select count(*) from timetable where period_id = ?", Integer.class, period));

        Long selectedVenue = tx.execute(status -> hall("Selected hall", 65));
        var withVenues = swapped.stream().map(assignment -> new StartAssignment(
                assignment.requirementId(), assignment.startTimeslotId(), List.of(selectedVenue))).toList();
        String saveBody = json.writeValueAsString(new DraftRequest(withVenues, true));
        String response = mvc.perform(post(api + "/drafts").session(admin).with(csrf())
                .contentType("application/json").content(saveBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.review.exams[0].venues[0].venueId").value(selectedVenue))
                .andReturn().getResponse().getContentAsString();
        var saved = json.readValue(response, Saved.class);
        assertNotNull(saved.timetableId());
        assertEquals(1, saved.version());
        assertEquals("DRAFT", sql.queryForObject("select status from timetable where timetable_id = ?",
                String.class, saved.timetableId()));
        assertEquals(2, sql.queryForObject("select count(*) from event where timetable_id = ?", Integer.class, saved.timetableId()));
        assertEquals(10, sql.queryForObject("select count(*) from event_slot s join event e using(event_id) where e.timetable_id = ?",
                Integer.class, saved.timetableId()));
        assertEquals(2, sql.queryForObject("select count(*) from event_allocation a join event e using(event_id) where e.timetable_id = ?",
                Integer.class, saved.timetableId()));
        assertEquals(80, sql.queryForObject("""
                select sum(g.allocated_count) from student_group_allocation g
                join event_allocation a using(allocation_id) join event e using(event_id) where e.timetable_id = ?
                """, Integer.class, saved.timetableId()));
        assertEquals(1, sql.queryForObject("select count(*) from audit_log where entity_id = ? and action = 'EXAM_DRAFT_SAVED'",
                Integer.class, saved.timetableId().toString()));
        assertTrue(service.open(period, saved.timetableId()).assignments().containsAll(withVenues));
        assertEquals(adminId, sql.queryForObject("select created_by_auth_user_id from timetable where timetable_id = ?",
                Long.class, saved.timetableId()));
        assertEquals(adminId, sql.queryForObject("select auth_user_id from audit_log where entity_id = ? and action = 'EXAM_DRAFT_SAVED'",
                Long.class, saved.timetableId().toString()));
        mvc.perform(get(api + "/drafts/" + saved.timetableId()).session(admin)).andExpect(status().isOk());
        assertEquals(2, service.save(period, swapped, true, adminId).version());

        sql.update("update users set enabled = false where id = ?", adminId);
        mvc.perform(post(api + "/drafts").session(admin).with(csrf())
                .contentType("application/json").content(saveBody)).andExpect(status().isForbidden());
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                        .param("username", email).param("password", PASSWORD))
                .andExpect(redirectedUrl("/admin/timetable.html")).andReturn().getRequest().getSession(false);
    }

    private long createExamPeriod(Long semesterId, MockHttpSession admin, MockHttpSession student) throws Exception {
        String path = "/api/admin/timetable-setup/setup";
        String weekend = json.writeValueAsString(new CreateSetupRequestDto(
                LocalDate.of(2026, 12, 19), "2025/2026", LocalDate.of(2026, 12, 20), "EXAM", List.of(semesterId), null));
        mvc.perform(post(path).session(admin).with(csrf()).contentType("application/json").content(weekend))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choose an exam period with at least one weekday that is not a holiday."));
        assertEquals(0, sql.queryForObject("select count(*) from calendar_period", Integer.class));

        var request = new CreateSetupRequestDto(LocalDate.of(2026, 12, 18), "2025/2026",
                LocalDate.of(2026, 12, 28), "EXAM", List.of(semesterId), List.of(LocalDate.of(2026, 12, 24)));
        String body = json.writeValueAsString(request);
        mvc.perform(post(path).session(student).with(csrf())
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post(path).session(admin).contentType("application/json").content(body)).andExpect(status().isForbidden());
        String response = mvc.perform(post(path).session(admin).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        long periodId = json.readValue(response, SetupResponse.class).periodId();
        assertEquals(List.of(LocalDate.of(2026, 12, 18), LocalDate.of(2026, 12, 21),
                        LocalDate.of(2026, 12, 22), LocalDate.of(2026, 12, 28)),
                sql.queryForList("select distinct date from timeslot where period_id = ? order by date",
                        LocalDate.class, periodId));
        assertEquals(60, sql.queryForObject("select count(*) from timeslot where period_id = ?",
                Integer.class, periodId));
        assertEquals(0, sql.queryForObject("""
                select count(*) from timeslot where period_id = ? and (
                    end_time - start_time <> interval '30 minutes'
                    or timeslot <> to_char(date + start_time, 'HH24:MI')
                    or not (
                        (start_time >= time '09:00' and end_time <= time '12:00')
                        or (start_time >= time '13:30' and end_time <= time '18:00')
                    )
                )
                """, Integer.class, periodId));
        return periodId;
    }

    private List<Long> fixture() {
        var admin = account(ADMIN_EMAIL, RoleName.ADMIN);
        account(STUDENT_EMAIL, RoleName.STUDENT);

        LocalDate day = LocalDate.of(2026, 12, 1);
        var department = new DepartmentEntity();
        department.setDepartmentName("Test Department");
        em.persist(department);
        var semester = new SemesterEntity();
        semester.setAcademicYear("2025/2026");
        semester.setSemesterName("1");
        semester.setStartingDate(day);
        em.persist(semester);
        hall("Test hall", 40);
        return List.of(semester.getSemesterId(), module("AA1201", department, semester), module("AA1202", department, semester), admin.getId());
    }

    private UserEntity account(String email, RoleName roleName) {
        var role = em.createQuery("select r from RoleEntity r where r.name = :name", RoleEntity.class)
                .setParameter("name", roleName).getSingleResult();
        var account = new UserEntity(email, email, passwords.encode(PASSWORD));
        account.addRole(role);
        em.persist(account);
        return account;
    }

    private Long hall(String name, int capacity) {
        var venue = new VenueEntity();
        venue.setVenueName(name);
        venue.setVenueType("Lecture Hall");
        venue.setExamCapacity(capacity);
        venue.setLectureCapacity(capacity + 10);
        venue.setPriority(1);
        em.persist(venue);
        return venue.getVenueId();
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
