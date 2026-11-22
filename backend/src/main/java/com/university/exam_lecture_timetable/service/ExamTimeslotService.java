package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.entity.CalendarPeriodEntity;
import com.university.exam_lecture_timetable.entity.TimeslotEntity;
import com.university.exam_lecture_timetable.exception.BadRequestException;
import com.university.exam_lecture_timetable.repository.TimeslotRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExamTimeslotService {

    //initializing timeslots.
    private static final LocalTime MORNING_START = LocalTime.of(9, 0);
    private static final LocalTime MORNING_END = LocalTime.NOON;
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 30);
    private static final LocalTime AFTERNOON_END = LocalTime.of(18, 0);
    private static final int SLOT_MINUTES = 30;
    private static final DateTimeFormatter SLOT_LABEL = DateTimeFormatter.ofPattern("HH:mm");

    private final TimeslotRepository timeslots;
    private final Set<LocalDate> configuredHolidays;

    //parse configured holiday dates into an unmodifiable set for exam scheduling.
    public ExamTimeslotService(TimeslotRepository timeslots,
                               @Value("${timetable.exam.holiday-dates:}") String holidayDates) {
        this.timeslots = timeslots;
        this.configuredHolidays = Arrays.stream(holidayDates.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).map(LocalDate::parse)
                .collect(Collectors.toUnmodifiableSet());
    }

    //Assigning slots by considering holidays and working days.
    void createFor(CalendarPeriodEntity period, List<LocalDate> holidayDates) {
        Set<LocalDate> excluded = new HashSet<>(configuredHolidays);
        if (holidayDates != null) {
            excluded.addAll(holidayDates);
        }

        List<TimeslotEntity> slots = new ArrayList<>();
        for (LocalDate date = period.getStartingDate(); !date.isAfter(period.getEndDate()); date = date.plusDays(1)) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !excluded.contains(date)) {
                addSession(slots, period, date, MORNING_START, MORNING_END);
                addSession(slots, period, date, AFTERNOON_START, AFTERNOON_END);
            }
            if (date.equals(period.getEndDate())) {
                break;
            }
        }

        if (slots.isEmpty()) {
            throw new BadRequestException("Choose an exam period with at least one weekday that is not a holiday.");
        }
        timeslots.saveAll(slots);
    }

    //create 30min slots and add them in to slots list.
    private static void addSession(List<TimeslotEntity> slots, CalendarPeriodEntity period, LocalDate date,
                                   LocalTime start, LocalTime end) {
        for (LocalTime time = start; time.isBefore(end); time = time.plusMinutes(SLOT_MINUTES)) {
            TimeslotEntity slot = new TimeslotEntity();
            slot.setPeriod(period);
            slot.setDate(date);
            slot.setTimeslot(time.format(SLOT_LABEL));
            slot.setStartTime(time);
            slot.setEndTime(time.plusMinutes(SLOT_MINUTES));
            slots.add(slot);
        }
    }
}
