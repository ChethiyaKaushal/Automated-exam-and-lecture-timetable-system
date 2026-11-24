"""Tests for the venue constraints, solved on their own with fixed exam start slots."""

from ortools.sat.python import cp_model

from app.schemas import SolverInput
from app.solver.venue_constraints import add_venue_constraints, extract_venue_allocations

OPTIMAL_OR_FEASIBLE = (cp_model.OPTIMAL, cp_model.FEASIBLE)


def make_input(exams, venues, slot_count=6, fixed_assignments=()):
    """One day of 30-minute slots from 09:00, ids 1..slot_count (slot id 1 has index 0)."""
    slots = []
    for i in range(slot_count):
        minutes = 9 * 60 + 30 * i
        slots.append({
            "timeslotId": i + 1,
            "date": "2026-12-01",
            "startTime": f"{minutes // 60:02d}:{minutes % 60:02d}",
            "endTime": f"{(minutes + 30) // 60:02d}:{(minutes + 30) % 60:02d}",
        })
    return SolverInput.model_validate({
        "periodId": 1,
        "exams": exams,
        "timeslots": slots,
        "venues": venues,
        "fixedAssignments": list(fixed_assignments),
    })


def exam(rid, groups, duration=60, venue_type=None):
    return {
        "requirementId": rid,
        "moduleId": rid,
        "moduleCode": f"M{rid}",
        "moduleName": f"Module {rid}",
        "curriculum": "C-23",
        "durationMinutes": duration,
        "requiredVenueType": venue_type,
        "studentCount": sum(groups.values()),
        "groups": [{"groupId": gid, "studentCount": count} for gid, count in groups.items()],
    }


def venue(vid, capacity, venue_type="LECTURE_HALL", unavailable=(), priority=1):
    return {
        "venueId": vid,
        "venueName": f"V{vid}",
        "venueType": venue_type,
        "examCapacity": capacity,
        "priority": priority,
        "unavailableTimeslotIds": list(unavailable),
    }


def solve(data, starts, free_starts=False):
    """starts: requirement_id -> start slot index. With free_starts, the index is only an upper bound."""
    model = cp_model.CpModel()
    last = len(data.timeslots) - 1
    start_vars = {}
    for e in data.exams:
        var = model.new_int_var(0, last - e.slot_count + 1, f"start_{e.requirement_id}")
        if not free_starts:
            model.add(var == starts[e.requirement_id])
        start_vars[e.requirement_id] = var
    venue_vars = add_venue_constraints(model, data, start_vars)
    solver = cp_model.CpSolver()
    solver.parameters.max_time_in_seconds = 10
    status = solver.solve(model)
    return status, solver, venue_vars, start_vars


def allocations(solver, venue_vars, data):
    return {e.requirement_id: extract_venue_allocations(solver, venue_vars, e) for e in data.exams}


# --- group splitting ---------------------------------------------------------

def test_large_group_is_split_over_several_halls():
    data = make_input(
        [exam(1, {10: 599})],
        [venue(1, 145), venue(2, 145), venue(3, 140), venue(4, 80), venue(5, 36), venue(6, 53)],
    )
    status, solver, vv, _ = solve(data, {1: 0})
    assert status in OPTIMAL_OR_FEASIBLE
    result = allocations(solver, vv, data)[1]
    assert sum(v.allocated_count for v in result) == 599
    assert len(result) == 6  # 599 needs every hall (total capacity 599)
    for v in result:
        assert sum(g.allocated_count for g in v.groups) == v.allocated_count


def test_each_group_is_fully_seated():
    data = make_input([exam(1, {10: 120, 11: 90})], [venue(1, 100), venue(2, 100), venue(3, 100)])
    status, solver, vv, _ = solve(data, {1: 0})
    assert status in OPTIMAL_OR_FEASIBLE
    per_group = {}
    for v in allocations(solver, vv, data)[1]:
        for g in v.groups:
            per_group[g.group_id] = per_group.get(g.group_id, 0) + g.allocated_count
    assert per_group == {10: 120, 11: 90}


# --- capacity ------------------------------------------------------------------

def test_not_enough_capacity_is_infeasible():
    data = make_input([exam(1, {10: 201})], [venue(1, 100), venue(2, 100)])
    status, *_ = solve(data, {1: 0})
    assert status == cp_model.INFEASIBLE


def test_allocation_never_exceeds_capacity():
    data = make_input([exam(1, {10: 250})], [venue(1, 100), venue(2, 100), venue(3, 100)])
    status, solver, vv, _ = solve(data, {1: 0})
    assert status in OPTIMAL_OR_FEASIBLE
    for v in allocations(solver, vv, data)[1]:
        assert v.allocated_count <= 100


# --- venue sharing (several exams in the same venue and session) --------------

def test_two_exams_share_a_hall_when_they_fit_together():
    # Old timetable: NCC held EC7205 (29) + EE7204 (13) + IS5123 (86) together.
    data = make_input(
        [exam(1, {10: 29}), exam(2, {20: 13}), exam(3, {30: 86})],
        [venue(1, 140)],
    )
    status, solver, vv, _ = solve(data, {1: 0, 2: 0, 3: 0})
    assert status in OPTIMAL_OR_FEASIBLE
    result = allocations(solver, vv, data)
    assert [v.venue_id for v in result[1]] == [1]
    assert [v.venue_id for v in result[2]] == [1]
    assert [v.venue_id for v in result[3]] == [1]


def test_shared_hall_capacity_counts_all_exams():
    data = make_input([exam(1, {10: 80}), exam(2, {20: 80})], [venue(1, 140)])
    status, *_ = solve(data, {1: 0, 2: 0})
    assert status == cp_model.INFEASIBLE


def test_overlapping_but_not_identical_times_share_capacity():
    # Exam 1 uses slots 0-3, exam 2 uses slots 2-5: they overlap in slots 2-3.
    data = make_input([exam(1, {10: 80}, duration=120), exam(2, {20: 80}, duration=120)], [venue(1, 140)])
    status, *_ = solve(data, {1: 0, 2: 2})
    assert status == cp_model.INFEASIBLE


def test_back_to_back_exams_can_reuse_the_full_hall():
    data = make_input([exam(1, {10: 140}), exam(2, {20: 140})], [venue(1, 140)])
    status, *_ = solve(data, {1: 0, 2: 2})
    assert status in OPTIMAL_OR_FEASIBLE


# --- availability -----------------------------------------------------------------

def test_unavailable_slot_blocks_the_venue():
    # Slot id 3 (index 2) is unavailable; a 90-minute exam from index 0 covers indexes 0, 1, 2.
    data = make_input([exam(1, {10: 50}, duration=90)], [venue(1, 100, unavailable=[3])])
    status, *_ = solve(data, {1: 0})
    assert status == cp_model.INFEASIBLE


def test_unavailable_venue_is_avoided():
    data = make_input([exam(1, {10: 50}, duration=90)], [venue(1, 100, unavailable=[3]), venue(2, 100)])
    status, solver, vv, _ = solve(data, {1: 0})
    assert status in OPTIMAL_OR_FEASIBLE
    assert [v.venue_id for v in allocations(solver, vv, data)[1]] == [2]


def test_solver_moves_exam_away_from_unavailable_slots():
    data = make_input([exam(1, {10: 50}, duration=60)], [venue(1, 100, unavailable=[1, 2])])
    status, solver, _, start_vars = solve(data, {}, free_starts=True)
    assert status in OPTIMAL_OR_FEASIBLE
    assert solver.value(start_vars[1]) >= 2


# --- venue type ---------------------------------------------------------------------

def test_computer_center_exam_only_uses_computer_centers():
    data = make_input(
        [exam(1, {10: 60}, venue_type="COMPUTER_CENTER")],
        [venue(1, 300, "LECTURE_HALL"), venue(2, 60, "COMPUTER_CENTER")],
    )
    status, solver, vv, _ = solve(data, {1: 0})
    assert status in OPTIMAL_OR_FEASIBLE
    assert [v.venue_id for v in allocations(solver, vv, data)[1]] == [2]


def test_drawing_office_exam_without_drawing_office_is_infeasible():
    data = make_input([exam(1, {10: 60}, venue_type="DRAWING_OFFICE")], [venue(1, 300, "LECTURE_HALL")])
    status, *_ = solve(data, {1: 0})
    assert status == cp_model.INFEASIBLE


def test_hall_types_are_interchangeable_option_b():
    data = make_input(
        [exam(1, {10: 200}, venue_type="AUDITORIUM")],
        [venue(1, 100, "AUDITORIUM"), venue(2, 100, "LECTURE_THEATRE"), venue(3, 500, "COMPUTER_CENTER")],
    )
    status, solver, vv, _ = solve(data, {1: 0})
    assert status in OPTIMAL_OR_FEASIBLE
    assert sorted(v.venue_id for v in allocations(solver, vv, data)[1]) == [1, 2]


def test_no_required_type_can_use_any_venue():
    data = make_input([exam(1, {10: 60})], [venue(1, 60, "COMPUTER_CENTER")])
    status, *_ = solve(data, {1: 0})
    assert status in OPTIMAL_OR_FEASIBLE


# --- fixed assignments ----------------------------------------------------------------

def test_fixed_venue_ids_are_respected():
    data = make_input(
        [exam(1, {10: 50})],
        [venue(1, 100), venue(2, 100)],
        fixed_assignments=[{"requirementId": 1, "startTimeslotId": 1, "venueIds": [2]}],
    )
    status, solver, vv, _ = solve(data, {1: 0})
    assert status in OPTIMAL_OR_FEASIBLE
    assert [v.venue_id for v in allocations(solver, vv, data)[1]] == [2]
