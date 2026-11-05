package com.university.exam_lecture_timetable;

import com.university.exam_lecture_timetable.controller.TimeTableSetupController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class ExamLectureTimetableApplicationTests {

	@Autowired
	private TimeTableSetupController controller;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void contextLoads() {
	}

	@Test
	void academicYearsEndpointReturnsDistinctYearsFromTheDatabase() throws Exception {
		var expectedYears = jdbc.queryForList("""
				select distinct academic_year from semester
				where academic_year is not null order by academic_year
				""", String.class);

		MockMvcBuilders.standaloneSetup(controller).build()
				.perform(get("/api/timetablesetup/academic-years"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].academicYear").value(expectedYears));
	}

}
