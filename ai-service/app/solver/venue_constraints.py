"""Venue allocation constraints for the exam timetable CP-SAT model.

These constraints are added to the SAME CpModel that holds the scheduling
variables, so the solver picks exam times and venues together in one Solve().

Contract with the scheduling module
-----------------------------------
The scheduling module creates one integer variable per exam:

    start_vars[requirement_id] = index of the exam's first slot in slot_order(data)

slot_order() sorts the timeslots by (date, startTime). An exam with
slot_count = k occupies the indexes start, start + 1, ..., start + k - 1.
The scheduling module is responsible for only allowing starts where those
k slots are contiguous in real time (same day, same session, no lunch gap).
"""

from dataclasses import dataclass, field

from ortools.sat.python import cp_model

from app.schemas.solver_input import Exam, Slot, SolverInput, Venue, VenueType
from app.schemas.solver_output import GroupAllocation, VenueAllocation

# These venue types must match exactly: an exam that needs a computer center
# can only be held in a computer center.
STRICT_VENUE_TYPES = {VenueType.COMPUTER_CENTER, VenueType.DRAWING_OFFICE}

# Ordinary halls. With STRICT_HALL_TYPES = False (Option B, pending team
# confirmation) an exam that asks for any hall type may use any hall type.
HALL_TYPES = {VenueType.LECTURE_HALL, VenueType.AUDITORIUM, VenueType.LECTURE_THEATRE}
STRICT_HALL_TYPES = False


@dataclass
class VenueVars:
    """Variables created by add_venue_constraints, used later to read the solution."""

    # (requirement_id, venue_id) -> True if the exam uses this venue
    use: dict[tuple[int, int], cp_model.IntVar] = field(default_factory=dict)
    # (requirement_id, venue_id) -> number of students of the exam in this venue
    alloc: dict[tuple[int, int], cp_model.IntVar] = field(default_factory=dict)
    # (requirement_id, group_id, venue_id) -> number of students of the group in this venue
    group_alloc: dict[tuple[int, int, int], cp_model.IntVar] = field(default_factory=dict)
    # requirement_id -> venue_ids the exam is allowed to use
    allowed_venues: dict[int, list[int]] = field(default_factory=dict)


def slot_order(data: SolverInput) -> list[Slot]:
    """Timeslots in time order. A start variable value is an index into this list."""
    return sorted(data.timeslots, key=lambda slot: (slot.date, slot.start_time))


def venue_type_allowed(exam: Exam, venue: Venue, strict_hall_types: bool = STRICT_HALL_TYPES) -> bool:
    required = exam.required_venue_type
    if required is None:
        return True
    if required in STRICT_VENUE_TYPES or strict_hall_types:
        return venue.venue_type == required
    # Option B: any hall type may replace another hall type.
    return venue.venue_type in HALL_TYPES


def allowed_venue_ids(data: SolverInput, exam: Exam, strict_hall_types: bool = STRICT_HALL_TYPES) -> list[int]:
    fixed = {fixed.requirement_id: fixed.venue_ids for fixed in data.fixed_assignments}
    fixed_venues = fixed.get(exam.requirement_id)
    return [
        venue.venue_id
        for venue in data.venues
        if venue_type_allowed(exam, venue, strict_hall_types)
        and (fixed_venues is None or venue.venue_id in fixed_venues)
    ]


def _unavailable_ranges(venue: Venue, index_of: dict[int, int]) -> list[tuple[int, int]]:
    """Merge the venue's unavailable slots into (start_index, length) ranges."""
    indexes = sorted(index_of[slot_id] for slot_id in venue.unavailable_timeslot_ids)
    ranges: list[tuple[int, int]] = []
    for index in indexes:
        if ranges and ranges[-1][0] + ranges[-1][1] == index:
            ranges[-1] = (ranges[-1][0], ranges[-1][1] + 1)
        else:
            ranges.append((index, 1))
    return ranges


def add_venue_constraints(
    model: cp_model.CpModel,
    data: SolverInput,
    start_vars: dict[int, cp_model.IntVar],
    min_students_per_venue: int = 1,
    strict_hall_types: bool = STRICT_HALL_TYPES,
) -> VenueVars:
    """Add all hard venue constraints to the shared model and return the venue variables."""
    venues = {venue.venue_id: venue for venue in data.venues}
    index_of = {slot.timeslot_id: index for index, slot in enumerate(slot_order(data))}
    vars_ = VenueVars()

    # Per venue: the time intervals and student counts that share it.
    intervals: dict[int, list[cp_model.IntervalVar]] = {venue_id: [] for venue_id in venues}
    demands: dict[int, list[cp_model.LinearExprT]] = {venue_id: [] for venue_id in venues}

    for exam in data.exams:
        rid = exam.requirement_id
        allowed = allowed_venue_ids(data, exam, strict_hall_types)
        vars_.allowed_venues[rid] = allowed

        for venue_id in allowed:
            capacity = venues[venue_id].exam_capacity
            use = model.new_bool_var(f"use_e{rid}_v{venue_id}")
            alloc = model.new_int_var(0, min(capacity, exam.student_count), f"alloc_e{rid}_v{venue_id}")
            vars_.use[rid, venue_id] = use
            vars_.alloc[rid, venue_id] = alloc

            # A venue is used exactly when it gets students.
            model.add(alloc <= min(capacity, exam.student_count) * use)
            model.add(alloc >= min(min_students_per_venue, exam.student_count) * use)

            # Group split: each group may be spread over several venues.
            group_parts = []
            for group in exam.groups:
                part = model.new_int_var(
                    0, min(capacity, group.student_count), f"galloc_e{rid}_g{group.group_id}_v{venue_id}"
                )
                vars_.group_alloc[rid, group.group_id, venue_id] = part
                group_parts.append(part)
            model.add(sum(group_parts) == alloc)

            # The exam occupies this venue for all of its slots, but only if the venue is used.
            interval = model.new_optional_fixed_size_interval_var(
                start_vars[rid], exam.slot_count, use, f"interval_e{rid}_v{venue_id}"
            )
            intervals[venue_id].append(interval)
            demands[venue_id].append(alloc)

        # Every student gets a seat.
        model.add(sum(vars_.alloc[rid, venue_id] for venue_id in allowed) == exam.student_count)
        for group in exam.groups:
            model.add(
                sum(vars_.group_alloc[rid, group.group_id, venue_id] for venue_id in allowed)
                == group.student_count
            )

    for venue_id, venue in venues.items():
        # Unavailable slots are blocked by a fixed interval that uses the full capacity,
        # so no exam can overlap them.
        for start, length in _unavailable_ranges(venue, index_of):
            blocked = model.new_fixed_size_interval_var(start, length, f"blocked_v{venue_id}_{start}")
            intervals[venue_id].append(blocked)
            demands[venue_id].append(venue.exam_capacity)

        # Venue sharing: several exams may run in the same venue at the same time,
        # as long as their students together fit in the exam capacity.
        if intervals[venue_id]:
            model.add_cumulative(intervals[venue_id], demands[venue_id], venue.exam_capacity)

    return vars_


def extract_venue_allocations(
    solver: cp_model.CpSolver, vars_: VenueVars, exam: Exam
) -> list[VenueAllocation]:
    """Read the venue split of one exam from a solved model, in the output schema format."""
    result = []
    for venue_id in vars_.allowed_venues[exam.requirement_id]:
        count = solver.value(vars_.alloc[exam.requirement_id, venue_id])
        if count == 0:
            continue
        groups = [
            GroupAllocation(group_id=group.group_id, allocated_count=allocated)
            for group in exam.groups
            if (allocated := solver.value(vars_.group_alloc[exam.requirement_id, group.group_id, venue_id])) > 0
        ]
        result.append(VenueAllocation(venue_id=venue_id, allocated_count=count, groups=groups))
    return result
