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


# ---------- SolverOutput ----------


def test_output_fixture_parses(output_data):
    parsed = SolverOutput.model_validate(output_data)

    assert parsed.status.value == "OPTIMAL"
    assert len(parsed.assignments) == 5
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


def test_output_rejects_optimal_without_assignments(output_data):
    output_data["assignments"] = []
    with pytest.raises(ValidationError, match="needs at least one assignment"):
        SolverOutput.model_validate(output_data)


def test_output_rejects_duplicate_requirement_id(output_data):
    assignment(output_data, 103)["requirementId"] = 102
    with pytest.raises(ValidationError, match="assigned more than once"):
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
