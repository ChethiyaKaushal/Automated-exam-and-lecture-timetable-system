import copy
import json
from pathlib import Path

import pytest
from pydantic import ValidationError

from app.schemas import SolverInput, SolverOutput

FIXTURES = Path(__file__).parent / "fixtures"


def load(name: str) -> dict:
    return json.loads((FIXTURES / name).read_text(encoding="utf-8"))


SMALL_INPUT = load("small_input.json")
SMALL_OUTPUT = load("small_output.json")


# Each test gets its own copy, so changing one field never leaks into another test.
@pytest.fixture
def input_data() -> dict:
    return copy.deepcopy(SMALL_INPUT)


@pytest.fixture
def output_data() -> dict:
    return copy.deepcopy(SMALL_OUTPUT)


def infeasible_output() -> dict:
    return {
        "status": "INFEASIBLE",
        "periodId": 1,
        "diagnostics": [
            {
                "code": "CAPACITY_TOO_SMALL",
                "source": "PRECHECK",
                "message": "CE2201 needs 300 seats but only 290 are free in any slot.",
                "requirementIds": [105],
                "details": {"requiredSeats": 300, "availableSeats": 290},
            }
        ],
        "stats": {"solveTimeSeconds": 0.0, "timeLimitSeconds": 60.0},
    }


def exam(data: dict, requirement_id: int) -> dict:
    return next(e for e in data["exams"] if e["requirementId"] == requirement_id)


def assignment(data: dict, requirement_id: int) -> dict:
    return next(a for a in data["assignments"] if a["requirementId"] == requirement_id)


# ---------- SolverInput ----------


def test_input_fixture_parses(input_data):
    parsed = SolverInput.model_validate(input_data)

    assert len(parsed.exams) == 5
    assert len(parsed.timeslots) == 30
    dumped = parsed.model_dump(by_alias=True)
    assert "conflictPairs" in dumped
    assert "requirementId" in dumped["exams"][0]
    assert "unavailableTimeslotIds" in dumped["venues"][0]


def test_slot_count_of_180_minute_exam(input_data):
    parsed = SolverInput.model_validate(input_data)
    drawing = next(e for e in parsed.exams if e.requirement_id == 101)

    assert drawing.duration_minutes == 180
    assert drawing.slot_count == 6


def test_165_minute_exam_parses_and_blocks_6_slots(input_data):
    exam(input_data, 105)["durationMinutes"] = 165
    parsed = SolverInput.model_validate(input_data)
    structures = next(e for e in parsed.exams if e.requirement_id == 105)

    assert structures.slot_count == 6


def test_45_minute_exam_blocks_2_slots(input_data):
    exam(input_data, 104)["durationMinutes"] = 45
    parsed = SolverInput.model_validate(input_data)
    programming = next(e for e in parsed.exams if e.requirement_id == 104)

    assert programming.slot_count == 2


def test_input_rejects_groups_sum_mismatch(input_data):
    exam(input_data, 102)["studentCount"] = 381
    with pytest.raises(ValidationError, match="groups have 380 students"):
        SolverInput.model_validate(input_data)


def test_input_rejects_conflict_pair_out_of_order(input_data):
    pair = input_data["conflictPairs"][0]
    pair["firstRequirementId"], pair["secondRequirementId"] = 105, 101
    with pytest.raises(ValidationError, match="must be less than"):
        SolverInput.model_validate(input_data)


def test_input_rejects_conflict_pair_with_unknown_requirement(input_data):
    input_data["conflictPairs"][0]["secondRequirementId"] = 999
    with pytest.raises(ValidationError, match="unknown requirementId 999"):
        SolverInput.model_validate(input_data)


def test_input_rejects_unknown_unavailable_timeslot(input_data):
    input_data["venues"][2]["unavailableTimeslotIds"] = [16, 99]
    with pytest.raises(ValidationError, match=r"unknown unavailableTimeslotIds \[99\]"):
        SolverInput.model_validate(input_data)


def test_input_rejects_duplicate_requirement_id(input_data):
    exam(input_data, 105)["requirementId"] = 104
    with pytest.raises(ValidationError, match="Duplicate requirementIds"):
        SolverInput.model_validate(input_data)


def test_input_rejects_shared_group_with_different_durations(input_data):
    exam(input_data, 103)["durationMinutes"] = 120
    with pytest.raises(ValidationError, match="different durations"):
        SolverInput.model_validate(input_data)


def test_input_rejects_unknown_field(input_data):
    exam(input_data, 101)["hasExam"] = True
    with pytest.raises(ValidationError, match="Extra inputs are not permitted"):
        SolverInput.model_validate(input_data)


def test_input_rejects_slot_not_30_minutes(input_data):
    input_data["timeslots"][0]["endTime"] = "09:45"
    with pytest.raises(ValidationError, match="exactly 30 minutes"):
        SolverInput.model_validate(input_data)


# ---------- Fixed assignments ----------


def test_input_fixed_assignments_parse(input_data):
    parsed = SolverInput.model_validate(input_data)

    fixed = {f.requirement_id: f.venue_ids for f in parsed.fixed_assignments}
    assert fixed == {101: [1], 104: [5]}
    assert "fixedAssignments" in parsed.model_dump(by_alias=True)


def test_input_fixture_has_one_fixed_assignment_per_typed_exam(input_data):
    parsed = SolverInput.model_validate(input_data)
    fixed_ids = [f.requirement_id for f in parsed.fixed_assignments]

    typed = [e.requirement_id for e in parsed.exams if e.required_venue_type is not None]
    assert typed == [101, 104]
    for rid in typed:
        assert fixed_ids.count(rid) == 1
    assert sorted(fixed_ids) == typed


def test_input_fixed_assignments_default_to_empty(input_data):
    # With no venue types, no fixed assignments are needed.
    del input_data["fixedAssignments"]
    for data in input_data["exams"]:
        data["requiredVenueType"] = None

    assert SolverInput.model_validate(input_data).fixed_assignments == []


def test_input_rejects_fixed_assignments_unknown_requirement(input_data):
    input_data["fixedAssignments"][0]["requirementId"] = 999
    with pytest.raises(ValidationError, match="Fixed assignment: unknown requirementId 999"):
        SolverInput.model_validate(input_data)


def test_input_rejects_fixed_assignments_unknown_venue(input_data):
    input_data["fixedAssignments"][0]["venueIds"] = [1, 99]
    with pytest.raises(ValidationError, match=r"requirement 101: unknown venueIds \[99\]"):
        SolverInput.model_validate(input_data)


def test_input_rejects_fixed_assignments_duplicate_venue(input_data):
    input_data["fixedAssignments"][0]["venueIds"] = [1, 1]
    with pytest.raises(ValidationError, match=r"venueIds \[1\] appear more than once"):
        SolverInput.model_validate(input_data)


def test_input_rejects_two_fixed_assignments_for_one_exam(input_data):
    input_data["fixedAssignments"].append({"requirementId": 101, "venueIds": [2]})
    with pytest.raises(ValidationError, match="Requirement 101 has more than one fixed assignment"):
        SolverInput.model_validate(input_data)


def test_input_rejects_fixed_assignments_empty_venue_list(input_data):
    input_data["fixedAssignments"][0]["venueIds"] = []
    with pytest.raises(ValidationError, match="at least 1 item"):
        SolverInput.model_validate(input_data)


def test_input_rejects_old_style_fixed_start_time(input_data):
    input_data["fixedAssignments"][0]["startTimeslotId"] = 1
    with pytest.raises(ValidationError, match="Extra inputs are not permitted"):
        SolverInput.model_validate(input_data)


def test_input_rejects_typed_exam_without_fixed_assignment(input_data):
    input_data["fixedAssignments"] = [f for f in input_data["fixedAssignments"] if f["requirementId"] != 104]
    with pytest.raises(
        ValidationError, match="Exam 104 has requiredVenueType COMPUTER_CENTER but no fixed assignment"
    ):
        SolverInput.model_validate(input_data)


def test_input_rejects_fixed_venue_of_other_type(input_data):
    # Venue 4 (NCC) is a LECTURE_HALL, not a DRAWING_OFFICE.
    input_data["fixedAssignments"][0]["venueIds"] = [1, 4]
    with pytest.raises(ValidationError, match=r"requirement 101: venues \[4\] are not DRAWING_OFFICE"):
        SolverInput.model_validate(input_data)


def test_input_rejects_hall_type_mismatch(input_data):
    # Exact match: an AUDITORIUM (venue 2) does not count as a LECTURE_HALL.
    exam(input_data, 102)["requiredVenueType"] = "LECTURE_HALL"
    input_data["fixedAssignments"].append({"requirementId": 102, "venueIds": [2, 4]})
    with pytest.raises(ValidationError, match=r"requirement 102: venues \[2\] are not LECTURE_HALL"):
        SolverInput.model_validate(input_data)


def test_input_rejects_fixed_assignment_for_untyped_exam(input_data):
    input_data["fixedAssignments"].append({"requirementId": 105, "venueIds": [2]})
    with pytest.raises(ValidationError, match="requirement 105: exam has no requiredVenueType"):
        SolverInput.model_validate(input_data)


# ---------- SolverOutput ----------


def test_output_fixture_parses(output_data):
    parsed = SolverOutput.model_validate(output_data)

    assert parsed.status.value == "OPTIMAL"
    assert len(parsed.assignments) == 5
    assert all(assignment.part == 1 for assignment in parsed.assignments)
    assert parsed.unscheduled == []
    dumped = parsed.model_dump(by_alias=True, mode="json")
    assert "startTimeslotId" in dumped["assignments"][0]
    assert dumped["stats"]["timeLimitSeconds"] == 60.0


def test_output_infeasible_with_diagnostic_parses():
    parsed = SolverOutput.model_validate(infeasible_output())

    assert parsed.assignments == []
    assert parsed.diagnostics[0].details["requiredSeats"] == 300


def test_output_rejects_infeasible_with_assignments(output_data):
    data = infeasible_output()
    data["assignments"] = copy.deepcopy(output_data["assignments"][:1])
    with pytest.raises(ValidationError, match="must not include assignments"):
        SolverOutput.model_validate(data)


def test_output_rejects_infeasible_without_diagnostics():
    data = infeasible_output()
    data["diagnostics"] = []
    with pytest.raises(ValidationError, match="needs at least one diagnostic"):
        SolverOutput.model_validate(data)


def test_output_rejects_unknown_without_time_limit_diagnostic():
    data = infeasible_output()
    data["status"] = "UNKNOWN"
    with pytest.raises(ValidationError, match="TIME_LIMIT_REACHED"):
        SolverOutput.model_validate(data)


def test_output_rejects_model_invalid_without_internal_error():
    data = infeasible_output()
    data["status"] = "MODEL_INVALID"
    with pytest.raises(ValidationError, match="INTERNAL_ERROR"):
        SolverOutput.model_validate(data)


def test_output_rejects_optimal_without_assignments_or_unscheduled(output_data):
    output_data["assignments"] = []
    assert output_data.get("unscheduled", []) == []
    with pytest.raises(ValidationError, match="needs at least one assignment or unscheduled exam"):
        SolverOutput.model_validate(output_data)


# ---------- Partial schedules (phase 2) ----------


def partial_output(output_data: dict) -> dict:
    """FEASIBLE phase-2 result: EE1102 (104) could not be placed, the other four exams are assigned."""
    data = copy.deepcopy(output_data)
    data["status"] = "FEASIBLE"
    data["assignments"] = [a for a in data["assignments"] if a["requirementId"] != 104]
    data["unscheduled"] = [
        {"requirementId": 104, "message": "EE1102 could not be placed within the given dates."}
    ]
    data["diagnostics"] = [
        {
            "code": "PARTIAL_SCHEDULE",
            "source": "SOLVER",
            "message": "1 of 5 exams could not be placed within the given dates.",
            "requirementIds": [104],
            "details": {"scheduledExams": 4, "unscheduledExams": 1},
        }
    ]
    return data


def test_output_partial_schedule_parses(output_data):
    parsed = SolverOutput.model_validate(partial_output(output_data))

    assert len(parsed.assignments) == 4
    assert [exam.requirement_id for exam in parsed.unscheduled] == [104]
    dumped = parsed.model_dump(by_alias=True, mode="json")
    assert dumped["unscheduled"][0]["requirementId"] == 104


def test_output_partial_schedule_with_only_unscheduled_parses(output_data):
    data = partial_output(output_data)
    data["assignments"] = []
    data["unscheduled"] = [
        {"requirementId": rid, "message": f"Exam {rid} could not be placed within the given dates."}
        for rid in (101, 102, 103, 104, 105)
    ]

    parsed = SolverOutput.model_validate(data)

    assert parsed.assignments == []
    assert len(parsed.unscheduled) == 5


def test_output_rejects_unscheduled_without_partial_diagnostic(output_data):
    data = partial_output(output_data)
    data["diagnostics"] = []
    with pytest.raises(ValidationError, match=r"unscheduled requirementIds \[104\] but no PARTIAL_SCHEDULE"):
        SolverOutput.model_validate(data)


def test_output_rejects_partial_diagnostic_without_unscheduled(output_data):
    data = partial_output(output_data)
    data["unscheduled"] = []
    with pytest.raises(ValidationError, match="PARTIAL_SCHEDULE diagnostic but no unscheduled exams"):
        SolverOutput.model_validate(data)


def test_output_rejects_exam_both_assigned_and_unscheduled(output_data):
    data = partial_output(output_data)
    data["unscheduled"][0]["requirementId"] = 105
    with pytest.raises(ValidationError, match=r"\[105\] are both assigned and unscheduled"):
        SolverOutput.model_validate(data)


def test_output_rejects_duplicate_unscheduled_requirement_id(output_data):
    data = partial_output(output_data)
    data["unscheduled"].append(copy.deepcopy(data["unscheduled"][0]))
    with pytest.raises(ValidationError, match=r"\[104\] are unscheduled more than once"):
        SolverOutput.model_validate(data)


def test_output_rejects_infeasible_with_unscheduled():
    data = infeasible_output()
    data["unscheduled"] = [{"requirementId": 105, "message": "CE2201 could not be placed."}]
    with pytest.raises(ValidationError, match="must not include unscheduled exams"):
        SolverOutput.model_validate(data)


def test_output_rejects_blank_unscheduled_message(output_data):
    data = partial_output(output_data)
    data["unscheduled"][0]["message"] = "   "
    with pytest.raises(ValidationError, match="Unscheduled exam 104: message must not be empty"):
        SolverOutput.model_validate(data)


def test_output_rejects_duplicate_requirement_id(output_data):
    assignment(output_data, 103)["requirementId"] = 102
    with pytest.raises(ValidationError, match=r"Requirement 102: parts \[1\] are assigned more than once"):
        SolverOutput.model_validate(output_data)


def test_output_rejects_venue_count_not_matching_groups(output_data):
    assignment(output_data, 101)["venues"][0]["allocatedCount"] = 139
    with pytest.raises(ValidationError, match="groups have 140 students but allocatedCount is 139"):
        SolverOutput.model_validate(output_data)


def test_output_rejects_student_count_not_matching_venues(output_data):
    assignment(output_data, 105)["studentCount"] = 301
    with pytest.raises(ValidationError, match="venues have 300 students but studentCount is 301"):
        SolverOutput.model_validate(output_data)


def test_output_accepts_end_time_inside_last_slot(output_data):
    drawing = assignment(output_data, 101)
    assert drawing["timeslotIds"] == [1, 2, 3, 4, 5, 6]
    drawing["endTime"] = "11:45"

    parsed = SolverOutput.model_validate(output_data)

    assert str(next(a for a in parsed.assignments if a.requirement_id == 101).end_time) == "11:45:00"


def test_output_rejects_end_time_too_early_for_slots(output_data):
    assignment(output_data, 101)["endTime"] = "10:00"
    with pytest.raises(ValidationError, match="60 minutes does not match 6 timeslots"):
        SolverOutput.model_validate(output_data)


def test_output_rejects_end_time_beyond_last_slot(output_data):
    assignment(output_data, 101)["endTime"] = "12:30"
    with pytest.raises(ValidationError, match="210 minutes does not match 6 timeslots"):
        SolverOutput.model_validate(output_data)


def test_output_rejects_first_timeslot_not_start(output_data):
    assignment(output_data, 104)["startTimeslotId"] = 2
    with pytest.raises(ValidationError, match="but startTimeslotId is 2"):
        SolverOutput.model_validate(output_data)


# ---------- Split drawing exams (Group A morning / Group B evening) ----------


def split_output(output_data: dict) -> dict:
    """ME1201 (101) split into part 1 at 09:00-12:00 (290 students) and part 2 at 13:30-16:30 (256)."""
    data = copy.deepcopy(output_data)
    data["assignments"] = [a for a in data["assignments"] if a["requirementId"] != 101]

    def part(number, start, end, slot_ids, venues):
        return {
            "requirementId": 101, "part": number, "studentCount": sum(count for _, _, count in venues),
            "date": "2026-12-01", "startTime": start, "endTime": end,
            "startTimeslotId": slot_ids[0], "timeslotIds": slot_ids,
            "venues": [
                {"venueId": venue, "allocatedCount": count, "groups": [{"groupId": group, "allocatedCount": count}]}
                for venue, group, count in venues
            ],
        }

    # Venue 1 = DO1, venue 6 = a second drawing office; groups 11-14 are illustrative.
    data["assignments"] += [
        part(1, "09:00", "12:00", [1, 2, 3, 4, 5, 6], [(1, 11, 140), (6, 12, 150)]),
        part(2, "13:30", "16:30", [7, 8, 9, 10, 11, 12], [(1, 13, 140), (6, 14, 116)]),
    ]
    return data


def split_parts(data: dict) -> list[dict]:
    return sorted((a for a in data["assignments"] if a["requirementId"] == 101), key=lambda a: a["part"])


def test_output_split_exam_parses(output_data):
    parsed = SolverOutput.model_validate(split_output(output_data))

    parts = sorted((a for a in parsed.assignments if a.requirement_id == 101), key=lambda a: a.part)
    assert [(p.part, p.student_count, f"{p.start_time:%H:%M}") for p in parts] == [
        (1, 290, "09:00"),
        (2, 256, "13:30"),
    ]
    dumped = parsed.model_dump(by_alias=True)
    assert all("part" in assignment for assignment in dumped["assignments"])


def test_output_rejects_same_part_twice(output_data):
    data = split_output(output_data)
    split_parts(data)[1]["part"] = 1
    with pytest.raises(ValidationError, match=r"Requirement 101: parts \[1\] are assigned more than once"):
        SolverOutput.model_validate(data)


def test_output_rejects_part_2_without_part_1(output_data):
    data = split_output(output_data)
    data["assignments"].remove(split_parts(data)[0])
    with pytest.raises(ValidationError, match=r"Requirement 101: parts \[2\] must be numbered 1 to 1"):
        SolverOutput.model_validate(data)


def test_output_rejects_split_parts_on_different_dates(output_data):
    data = split_output(output_data)
    split_parts(data)[1]["date"] = "2026-12-02"
    with pytest.raises(ValidationError, match="Requirement 101: split parts must be on the same date"):
        SolverOutput.model_validate(data)


def test_output_rejects_part_2_starting_before_part_1_ends(output_data):
    data = split_output(output_data)
    split_parts(data)[1].update(startTime="11:00", endTime="14:00")
    with pytest.raises(ValidationError, match="part 2 starts at 11:00, before part 1 ends at 12:00"):
        SolverOutput.model_validate(data)


def test_output_rejects_part_0(output_data):
    assignment(output_data, 101)["part"] = 0
    with pytest.raises(ValidationError, match="greater than or equal to 1"):
        SolverOutput.model_validate(output_data)


def test_output_rejects_split_exam_also_unscheduled(output_data):
    data = split_output(output_data)
    data["status"] = "FEASIBLE"
    data["unscheduled"] = [{"requirementId": 101, "message": "ME1201 could not be placed within the given dates."}]
    data["diagnostics"] = [
        {"code": "PARTIAL_SCHEDULE", "source": "SOLVER", "message": "1 of 5 exams could not be placed."}
    ]
    with pytest.raises(ValidationError, match=r"\[101\] are both assigned and unscheduled"):
        SolverOutput.model_validate(data)
