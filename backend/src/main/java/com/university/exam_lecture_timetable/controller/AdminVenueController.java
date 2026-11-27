package com.university.exam_lecture_timetable.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.university.exam_lecture_timetable.entity.TimeslotEntity;
import com.university.exam_lecture_timetable.entity.VenueAvailabilityEntity;
import com.university.exam_lecture_timetable.entity.VenueAvailabilityId;
import com.university.exam_lecture_timetable.entity.VenueEntity;
import com.university.exam_lecture_timetable.entity.VenueType;
import com.university.exam_lecture_timetable.exception.ConflictException;
import com.university.exam_lecture_timetable.exception.ResourceNotFoundException;
import com.university.exam_lecture_timetable.repository.TimeslotRepository;
import com.university.exam_lecture_timetable.repository.VenueAvailabilityRepository;
import com.university.exam_lecture_timetable.repository.VenueRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/admin/venues")
@Transactional
public class AdminVenueController {

    private final VenueRepository venues;
    private final VenueAvailabilityRepository availability;
    private final TimeslotRepository slots;

    public AdminVenueController(VenueRepository venues, VenueAvailabilityRepository availability, TimeslotRepository slots) {
        this.venues = venues;
        this.availability = availability;
        this.slots = slots;
    }

    public record VenueInput(@NotBlank
            @Size(max = 255) String venueName, @NotNull VenueType venueType,
            @NotNull
            @PositiveOrZero Integer lectureCapacity, @PositiveOrZero Integer examCapacity,
            @NotNull
            @PositiveOrZero Integer priority, @NotNull Boolean isAvailableForExams) {}

    public record VenueView(Long venueId, String venueName, VenueType venueType, Integer lectureCapacity,
            Integer examCapacity, Integer priority, Boolean isAvailableForExams) {}

    public record SlotView(Long timeslotId, Long periodId, String date, String startTime, String endTime, String timeslot) {}

    private VenueEntity venue(Long id) {
        return venues.findById(id).orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
    }

    private VenueView view(VenueEntity v) {
        return new VenueView(v.getVenueId(), v.getVenueName(), v.getVenueType(), v.getLectureCapacity(), v.getExamCapacity(), v.getPriority(), v.getIsAvailableForExams());
    }

    private VenueView save(VenueEntity v, VenueInput i) {
        v.setVenueName(i.venueName().trim());
        v.setVenueType(i.venueType());
        v.setLectureCapacity(i.lectureCapacity());
        v.setExamCapacity(i.examCapacity());
        v.setPriority(i.priority());
        v.setIsAvailableForExams(i.isAvailableForExams());
        return view(venues.saveAndFlush(v));
    }

    @GetMapping
    public List<VenueView> list() {
        return venues.findAll().stream().map(this::view).toList();
    }

    @GetMapping("/{id}")
    public VenueView detail(@PathVariable Long id) {
        return view(venue(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenueView create(@Valid @RequestBody VenueInput input) {
        return save(new VenueEntity(), input);
    }

    @PutMapping("/{id}")
    public VenueView update(@PathVariable Long id, @Valid @RequestBody VenueInput input) {
        return save(venue(id), input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        VenueEntity v = venue(id);
        if (!v.getAllocations().isEmpty()) {
            throw new ConflictException("Venue has existing allocations. Disable it instead.");
        }
        if (!v.getAvailabilities().isEmpty()) {
            throw new ConflictException("Remove venue availability records before deleting.");
        }
        venues.delete(v);
        venues.flush();
    }

    private SlotView slotView(TimeslotEntity s) {
        return new SlotView(s.getTimeslotId(), s.getPeriod().getPeriodId(), s.getDate().toString(), s.getStartTime() == null ? null : s.getStartTime().toString(), s.getEndTime() == null ? null : s.getEndTime().toString(), s.getTimeslot());
    }

    @GetMapping("/timeslots")
    public List<SlotView> timeslots() {
        return slots.findAll().stream().map(this::slotView).toList();
    }

    @GetMapping("/{id}/blocks")
    public List<SlotView> blocks(@PathVariable Long id) {
        venue(id);
        return availability.findByIdVenueIdAndIsAvailableFalse(id).stream().map(a -> slotView(a.getTimeslot())).toList();
    }

    @PutMapping("/{id}/blocks/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@PathVariable Long id, @PathVariable Long slotId) {
        VenueEntity v = venue(id);
        TimeslotEntity s = slots.findById(slotId).orElseThrow(() -> new ResourceNotFoundException("Timeslot not found"));
        if (v.getAllocations().stream().anyMatch(a -> a.getEvent().getEventSlots().stream().anyMatch(es -> es.getTimeslot().getTimeslotId().equals(slotId)))) {
            throw new ConflictException("This timeslot already has a venue allocation.");
        }
        VenueAvailabilityId key = new VenueAvailabilityId(id, slotId);
        VenueAvailabilityEntity a = availability.findById(key).orElseGet(() -> new VenueAvailabilityEntity(v, s, false));
        a.setIsAvailable(false);
        availability.saveAndFlush(a);
    }

    @DeleteMapping("/{id}/blocks/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable Long id, @PathVariable Long slotId) {
        venue(id);
        VenueAvailabilityId key = new VenueAvailabilityId(id, slotId);
        availability.findById(key)
                .filter(a -> !a.getIsAvailable())
                .ifPresent(availability::delete);
    }

    @ExceptionHandler(ConflictException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> handleVenueConflict(
            ConflictException ex) {

        return org.springframework.http.ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(java.util.Map.of(
                        "status", 409,
                        "error", "Conflict",
                        "message", ex.getMessage()
                ));
    }
}
