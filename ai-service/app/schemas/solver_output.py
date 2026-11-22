"""Output schema for the exam timetable solver."""

from datetime import date, time
from enum import Enum

from pydantic import Field, field_validator, model_validator

from app.schemas.solver_input import SLOT_MINUTES, CamelModel, _duplicates


class SolveStatus(str, Enum):

    OPTIMAL = "OPTIMAL"
    FEASIBLE = "FEASIBLE"
    INFEASIBLE = "INFEASIBLE"
    UNKNOWN = "UNKNOWN"
    MODEL_INVALID = "MODEL_INVALID"


class DiagnosticCode(str, Enum):
    NO_VENUE_OF_TYPE = "NO_VENUE_OF_TYPE"
    CAPACITY_TOO_SMALL = "CAPACITY_TOO_SMALL"
    EXAM_LONGER_THAN_SESSION = "EXAM_LONGER_THAN_SESSION"
    NO_VALID_START = "NO_VALID_START"
    FIXED_ASSIGNMENT_INVALID = "FIXED_ASSIGNMENT_INVALID"
    FIXED_ASSIGNMENT_CONFLICT = "FIXED_ASSIGNMENT_CONFLICT"
    NOT_ENOUGH_DAYS = "NOT_ENOUGH_DAYS"
    CONFLICTING_RULES = "CONFLICTING_RULES"
    TIME_LIMIT_REACHED = "TIME_LIMIT_REACHED"
    INTERNAL_ERROR = "INTERNAL_ERROR"
    NO_SOLUTION_FOUND = "NO_SOLUTION_FOUND"


class DiagnosticSource(str, Enum):
    PRECHECK = "PRECHECK"  # found before solving
    SOLVER = "SOLVER"  # found by the solver


SOLVED = {SolveStatus.OPTIMAL, SolveStatus.FEASIBLE}


class GroupAllocation(CamelModel):
    group_id: int
    allocated_count: int = Field(gt=0)


class VenueAllocation(CamelModel):
    venue_id: int
    allocated_count: int = Field(gt=0)
    groups: list[GroupAllocation] = Field(min_length=1)

    @model_validator(mode="after")
    def check_venue(self) -> "VenueAllocation":
        group_total = sum(group.allocated_count for group in self.groups)
        if group_total != self.allocated_count:
            raise ValueError(
                f"Venue {self.venue_id}: groups have {group_total} students but allocatedCount is "
                f"{self.allocated_count}."
            )
        repeated = _duplicates(group.group_id for group in self.groups)
        if repeated:
            raise ValueError(f"Venue {self.venue_id}: groupIds {repeated} appear more than once.")
        return self


class Assignment(CamelModel):
    requirement_id: int
    student_count: int = Field(gt=0)
    date: date
    start_time: time
    end_time: time
    start_timeslot_id: int

    timeslot_ids: list[int] = Field(min_length=1)

    venues: list[VenueAllocation] = Field(min_length=1)

    @model_validator(mode="after")
    def check_assignment(self) -> "Assignment":
        rid = self.requirement_id
        if self.end_time <= self.start_time:
            raise ValueError(f"Assignment for requirement {rid}: endTime must be after startTime.")
        duration = (self.end_time.hour * 60 + self.end_time.minute) - (
            self.start_time.hour * 60 + self.start_time.minute
        )
        slots = len(self.timeslot_ids)
        if not (slots - 1) * SLOT_MINUTES < duration <= slots * SLOT_MINUTES:
            raise ValueError(
                f"Assignment for requirement {rid}: {duration} minutes does not match {slots} timeslots "
                f"(expected more than {(slots - 1) * SLOT_MINUTES} and at most {slots * SLOT_MINUTES})."
            )
        repeated = _duplicates(self.timeslot_ids)
        if repeated:
            raise ValueError(f"Assignment for requirement {rid}: timeslotIds {repeated} appear more than once.")
        if self.timeslot_ids[0] != self.start_timeslot_id:
            raise ValueError(
                f"Assignment for requirement {rid}: timeslotIds starts with {self.timeslot_ids[0]} "
                f"but startTimeslotId is {self.start_timeslot_id}."
            )
        repeated = _duplicates(venue.venue_id for venue in self.venues)
        if repeated:
            raise ValueError(f"Assignment for requirement {rid}: venueIds {repeated} appear more than once.")
        venue_total = sum(venue.allocated_count for venue in self.venues)
        if venue_total != self.student_count:
            raise ValueError(
                f"Assignment for requirement {rid}: venues have {venue_total} students but studentCount is "
                f"{self.student_count}."
            )
        return self


class Diagnostic(CamelModel):
    code: DiagnosticCode
    source: DiagnosticSource

    message: str
    requirement_ids: list[int] = Field(default_factory=list)
    venue_ids: list[int] = Field(default_factory=list)
    timeslot_ids: list[int] = Field(default_factory=list)

    details: dict[str, int | float | str | bool] = Field(default_factory=dict)

    @field_validator("message")
    @classmethod
    def check_message(cls, message: str) -> str:
        if not message.strip():
            raise ValueError("Diagnostic message must not be empty.")
        return message


class SolverStats(CamelModel):
    objective_value: float | None = None
    best_bound: float | None = None
    solve_time_seconds: float = Field(ge=0)
    time_limit_seconds: float = Field(gt=0)


class SolverOutput(CamelModel):
    status: SolveStatus
    period_id: int
    assignments: list[Assignment] = Field(default_factory=list)
    diagnostics: list[Diagnostic] = Field(default_factory=list)
    stats: SolverStats

    @model_validator(mode="after")
    def check_status(self) -> "SolverOutput":
        status = self.status.value
        if self.status in SOLVED:
            if not self.assignments:
                raise ValueError(f"Status {status} needs at least one assignment.")
            repeated = _duplicates(assignment.requirement_id for assignment in self.assignments)
            if repeated:
                raise ValueError(f"requirementIds {repeated} are assigned more than once.")
            return self

        # All or nothing, and the admin always gets a reason.
        if self.assignments:
            raise ValueError(f"Status {status} must not include assignments.")
        if not self.diagnostics:
            raise ValueError(f"Status {status} needs at least one diagnostic.")
        codes = {diagnostic.code for diagnostic in self.diagnostics}
        required = {
            SolveStatus.UNKNOWN: DiagnosticCode.TIME_LIMIT_REACHED,
            SolveStatus.MODEL_INVALID: DiagnosticCode.INTERNAL_ERROR,
        }.get(self.status)
        if required is not None and required not in codes:
            raise ValueError(f"Status {status} needs a diagnostic with code {required.value}.")
        return self
