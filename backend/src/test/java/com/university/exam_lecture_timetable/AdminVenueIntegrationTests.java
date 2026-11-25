package com.university.exam_lecture_timetable;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.university.exam_lecture_timetable.dto.request.CreateSetupRequestDto;
import com.university.exam_lecture_timetable.entity.SemesterEntity;
import com.university.exam_lecture_timetable.exception.ConflictException;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:venue_integration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class AdminVenueIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    EntityManager em;

    @Autowired
    TransactionTemplate tx;

    private static final String API = "/api/admin/venues";

    private String venueBody(String name, int capacity) {
        return """
            {
                "venueName": "%s",
                "venueType": "LECTURE_HALL",
                "lectureCapacity": %d,
                "examCapacity": 50,
                "priority": 1,
                "isAvailableForExams": true
            }
            """.formatted(name, capacity);
    }

    private long createVenue(String name) throws Exception {

        String response = mvc.perform(
                post(API)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(venueBody(name, 100))
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return json.readTree(response)
                .get("venueId").asLong();
    }

    // Test 1: Venue CRUD
    @Test
    void venueCrudWorks() throws Exception {

        long id = createVenue("Test Lecture Hall");

        mvc.perform(
                get(API)
                        .with(user("admin").roles("ADMIN"))
        )
                .andExpect(status().isOk());

        mvc.perform(
                get(API + "/" + id)
                        .with(user("admin").roles("ADMIN"))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venueName")
                        .value("Test Lecture Hall"));

        mvc.perform(
                put(API + "/" + id)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(venueBody("Updated Hall", 150))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venueName")
                        .value("Updated Hall"))
                .andExpect(jsonPath("$.lectureCapacity")
                        .value(150));

        mvc.perform(
                delete(API + "/" + id)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
        )
                .andExpect(status().isNoContent());

        mvc.perform(
                get(API + "/" + id)
                        .with(user("admin").roles("ADMIN"))
        )
                .andExpect(status().isNotFound());
    }

    // Test 2: Input validation
    @Test
    void invalidVenueIsRejected() throws Exception {

        mvc.perform(
                post(API)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(venueBody("", -10))
        )
                .andExpect(status().isBadRequest());
    }

    // Test 3: Admin authorization
    @Test
    void studentCannotCreateVenue() throws Exception {

        mvc.perform(
                post(API)
                        .with(user("student").roles("STUDENT"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(venueBody("Restricted Hall", 100))
        )
                .andExpect(status().isForbidden());
    }

    // Test 4: Venue availability
    @Test
    void blockAndUnblockTimeslot() throws Exception {

        long venueId = createVenue("Availability Test Hall");

        Long semesterId = tx.execute(status -> {

            SemesterEntity semester = new SemesterEntity();

            semester.setAcademicYear("2025/2026");
            semester.setSemesterName("1");
            semester.setStartingDate(
                    LocalDate.of(2026, 12, 1)
            );

            em.persist(semester);

            return semester.getSemesterId();
        });

        var request = new CreateSetupRequestDto(
                LocalDate.of(2026, 12, 18),
                "2025/2026",
                LocalDate.of(2026, 12, 18),
                "EXAM",
                List.of(semesterId),
                List.of()
        );

        mvc.perform(
                post("/api/admin/timetable-setup/setup")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(json.writeValueAsString(request))
        )
                .andExpect(status().isCreated());

        String slots = mvc.perform(
                get(API + "/timeslots")
                        .with(user("admin").roles("ADMIN"))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timeslotId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long slotId = json.readTree(slots)
                .get(0)
                .get("timeslotId")
                .asLong();

        String blocks = API + "/" + venueId + "/blocks";

        mvc.perform(
                put(blocks + "/" + slotId)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
        )
                .andExpect(status().isNoContent());

        mvc.perform(
                get(blocks)
                        .with(user("admin").roles("ADMIN"))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timeslotId")
                        .value(slotId));

        mvc.perform(
                delete(API + "/" + venueId)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
        )
                .andExpect(status().isConflict());

        mvc.perform(
                delete(blocks + "/" + slotId)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
        )
                .andExpect(status().isNoContent());

        mvc.perform(
                get(blocks)
                        .with(user("admin").roles("ADMIN"))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(
                delete(API + "/" + venueId)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
        )
                .andExpect(status().isNoContent());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<java.util.Map<String, Object>> handleVenueConflict(
            ConflictException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(java.util.Map.of(
                        "status", 409,
                        "error", "Conflict",
                        "message", ex.getMessage()
                ));
    }
}
