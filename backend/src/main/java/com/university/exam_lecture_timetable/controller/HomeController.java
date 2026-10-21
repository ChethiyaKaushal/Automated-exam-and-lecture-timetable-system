
package com.university.exam_lecture_timetable.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Exam and Lecture Timetable Backend is running!";
    }
}
