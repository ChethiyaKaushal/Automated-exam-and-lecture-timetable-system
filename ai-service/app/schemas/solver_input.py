"""Input schema for the exam timetable solver."""

from collections import Counter
from datetime import date, datetime, time
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator
from pydantic.alias_generators import to_camel

SLOT_MINUTES = 30


class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid"
    )


class VenueType(str, Enum):
    COMPUTER_CENTER = "COMPUTER_CENTER"
    LECTURE_HALL = "LECTURE_HALL"
    AUDITORIUM = "AUDITORIUM"
    LECTURE_THEATRE = "LECTURE_THEATRE"
    DRAWING_OFFICE = "DRAWING_OFFICE"


class ConflictType(str, Enum):
    # PROPER + PROPER Exams can't held on same day
    DIFFERENT_DAY = "DIFFERENT_DAY"
    # Any other common student (PROPER+REPEAT, REPEAT+REPEAT): no overlap, but can same day allowed
    NO_TIME_OVERLAP = "NO_TIME_OVERLAP"


def _duplicates(values) -> list:
    return sorted(
        value for value,
        count in Counter(values).items() if count > 1)


class Group(CamelModel):
    group_id: int
    student_count: int = Field(gt=0)


class Exam(CamelModel):
    requirement_id: int
    module_id: int
    module_code: str
    module_name: str
    curriculum: str

    department_id: int | None = None
    semester_id: int | None = None

    duration_minutes: int = Field(gt=0)

    exam_gap_days: int = Field(default=0, ge=0)

    required_venue_type: VenueType | None = None
    student_count: int = Field(gt=0)
    groups: list[Group] = Field(min_length=1)

    @property
    def slot_count(self) -> int:
        # Round up: a 165-minute exam blocks 6 slots and the last partial slot stays empty.
        return -(-self.duration_minutes // SLOT_MINUTES)

    @model_validator(mode="after")
    def check_exam(self) -> "Exam":
        rid = self.requirement_id
        group_total = sum(group.student_count for group in self.groups)
        if group_total != self.student_count:
            raise ValueError(
                f"Exam {rid}: groups have {group_total} students but studentCount is {self.student_count}."
            )
        repeated = _duplicates(group.group_id for group in self.groups)
        if repeated:
            raise ValueError(f"Exam {rid}: groupIds {repeated} appear more than once.")
        return self


class Slot(CamelModel):
    timeslot_id: int
    date: date
    start_time: time
    end_time: time

    @model_validator(mode="after")
    def check_slot(self) -> "Slot":
        if self.end_time <= self.start_time:
            raise ValueError(f"Timeslot {self.timeslot_id}: endTime must be after startTime.")
        start = datetime.combine(
            self.date, self.start_time
        )
        end = datetime.combine(
            self.date, self.end_time
        )
        if (end - start).total_seconds() != SLOT_MINUTES * 60:
            raise ValueError(f"Timeslot {self.timeslot_id}: must be exactly {SLOT_MINUTES} minutes long.")
        return self


class Venue(CamelModel):
    venue_id: int
    venue_name: str
    venue_type: VenueType
    exam_capacity: int = Field(gt=0)
    priority: int = Field(ge=1)
    unavailable_timeslot_ids: list[int] = Field(default_factory=list)


class ConflictPair(CamelModel):
    first_requirement_id: int
    second_requirement_id: int
    conflict_type: ConflictType

    first_has_proper_students: bool
    second_has_proper_students: bool

    @model_validator(mode="after")
    def check_pair(self) -> "ConflictPair":
        first, second = self.first_requirement_id, self.second_requirement_id
        if first == second:
            raise ValueError(f"Conflict pair ({first}, {second}): an exam cannot conflict with itself.")
        # Canonical order, so the same pair is never sent twice.
        if first > second:
            raise ValueError(
                f"Conflict pair ({first}, {second}): firstRequirementId must be less than secondRequirementId."
            )
        return self


class SharedExamGroup(CamelModel):
    # Same paper: every exam in the group starts in the same slot.
    requirement_ids: list[int] = Field(min_length=2)

    @field_validator("requirement_ids")
    @classmethod
    def check_unique(cls, ids: list[int]) -> list[int]:
        repeated = _duplicates(ids)
        if repeated:
            raise ValueError(f"Shared exam group: requirementIds {repeated} appear more than once.")
        return ids


class FixedAssignment(CamelModel):
    # The exam must use exactly these venues; the solver chooses the day and session.
    requirement_id: int
    venue_ids: list[int] = Field(min_length=1)

    @field_validator("venue_ids")
    @classmethod
    def check_unique(cls, ids: list[int]) -> list[int]:
        repeated = _duplicates(ids)
        if repeated:
            raise ValueError(f"Fixed assignment: venueIds {repeated} appear more than once.")
        return ids


class SolverInput(CamelModel):
    period_id: int
    exams: list[Exam] = Field(min_length=1)
    timeslots: list[Slot] = Field(min_length=1)
    venues: list[Venue] = Field(min_length=1)
    conflict_pairs: list[ConflictPair] = Field(default_factory=list)
    shared_exam_groups: list[SharedExamGroup] = Field(default_factory=list)
    fixed_assignments: list[FixedAssignment] = Field(default_factory=list)

    @model_validator(mode="after")
    def check_references(self) -> "SolverInput":
        self._check_unique_ids()

        exams = {
            exam.requirement_id: exam
            for exam in self.exams
        }
        slot_ids = {
            slot.timeslot_id
            for slot in self.timeslots
        }
        venue_ids = {
            venue.venue_id
            for venue in self.venues
        }

        for venue in self.venues:
            unknown = sorted(set(venue.unavailable_timeslot_ids) - slot_ids)
            if unknown:
                raise ValueError(f"Venue {venue.venue_id}: unknown unavailableTimeslotIds {unknown}.")

        seen_pairs = set()
        for pair in self.conflict_pairs:
            key = (pair.first_requirement_id, pair.second_requirement_id)
            for rid in key:
                if rid not in exams:
                    raise ValueError(f"Conflict pair {key}: unknown requirementId {rid}.")
            if key in seen_pairs:
                raise ValueError(f"Conflict pair {key} appears more than once.")
            seen_pairs.add(key)

        shared_owner: dict[int, int] = {}
        for index, group in enumerate(self.shared_exam_groups):
            for rid in group.requirement_ids:
                if rid not in exams:
                    raise ValueError(f"Shared exam group {index}: unknown requirementId {rid}.")
                if rid in shared_owner:
                    raise ValueError(
                        f"Requirement {rid} is in shared exam groups {shared_owner[rid]} and {index}."
                    )
                shared_owner[rid] = index
            durations = {exams[rid].duration_minutes for rid in group.requirement_ids}
            if len(durations) > 1:
                raise ValueError(
                    f"Shared exam group {index}: exams {group.requirement_ids} have different durations "
                    f"{sorted(durations)}."
                )

        fixed_seen = set()
        for fixed in self.fixed_assignments:
            rid = fixed.requirement_id
            if rid not in exams:
                raise ValueError(f"Fixed assignment: unknown requirementId {rid}.")
            if rid in fixed_seen:
                raise ValueError(f"Requirement {rid} has more than one fixed assignment.")
            fixed_seen.add(rid)
            unknown = sorted(set(fixed.venue_ids) - venue_ids)
            if unknown:
                raise ValueError(f"Fixed assignment for requirement {rid}: unknown venueIds {unknown}.")
        return self

    def _check_unique_ids(self) -> None:
        checks = [
            ("requirementIds", [exam.requirement_id for exam in self.exams]),
            ("timeslotIds", [slot.timeslot_id for slot in self.timeslots]),
            ("venueIds", [venue.venue_id for venue in self.venues]),
        ]
        for name, values in checks:
            repeated = _duplicates(values)
            if repeated:
                raise ValueError(f"Duplicate {name}: {repeated}.")
        repeated_starts = _duplicates((slot.date, slot.start_time) for slot in self.timeslots)
        if repeated_starts:
            date_value, start = repeated_starts[0]
            raise ValueError(f"More than one timeslot starts at {date_value} {start}.")
