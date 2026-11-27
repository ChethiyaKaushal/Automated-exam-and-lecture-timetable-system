"""Builds the solver context and runs the CP-SAT solver"""

from dataclasses import dataclass, field

from ortools.sat.python import cp_model

from app.schemas.solver_input import Slot, SolverInput
from app.schemas.solver_output import Diagnostic, SolveStatus

_STATUS = {
    cp_model.OPTIMAL: SolveStatus.OPTIMAL,
    cp_model.FEASIBLE: SolveStatus.FEASIBLE,
    cp_model.INFEASIBLE: SolveStatus.INFEASIBLE,
    cp_model.UNKNOWN: SolveStatus.UNKNOWN,
    cp_model.MODEL_INVALID: SolveStatus.MODEL_INVALID,
}


@dataclass
class SolverContext:
    """Stores the data needed by the solver."""

    data: SolverInput
    model: cp_model.CpModel
    sessions: list[list[Slot]]
    slot_by_id: dict[int, Slot]
    day_index: dict[int, int]
    session_of: dict[int, int]
    exam_vars: dict = field(default_factory=dict)
    venue_vars: dict = field(default_factory=dict)
    objective_terms: list = field(default_factory=list)
    diagnostics: list[Diagnostic] = field(default_factory=list)


@dataclass
class RunResult:
    """Stores the result returned by the solver."""

    solver: cp_model.CpSolver
    status: SolveStatus
    solve_time_seconds: float
    objective_value: float | None
    best_bound: float | None


def build_context(data: SolverInput) -> SolverContext:
    """Prepares the input data before solving."""

    slots = sorted(data.timeslots, key=lambda slot: (slot.date, slot.start_time))

    sessions: list[list[Slot]] = []
    for slot in slots:
        previous = sessions[-1][-1] if sessions else None
        if previous and previous.date == slot.date and previous.end_time == slot.start_time:
            sessions[-1].append(slot)
        else:
            sessions.append([slot])


    first_date = slots[0].date
    return SolverContext(
        data=data,
        model=cp_model.CpModel(),
        sessions=sessions,
        slot_by_id={slot.timeslot_id: slot for slot in slots},
        day_index={slot.timeslot_id: (slot.date - first_date).days for slot in slots},
        session_of={slot.timeslot_id: index for index, session in enumerate(sessions) for slot in session},
    )


def run(ctx: SolverContext, time_limit_seconds: float) -> RunResult:
    """Runs the solver and returns the result."""
    
    solver = cp_model.CpSolver()
    solver.parameters.max_time_in_seconds = float(time_limit_seconds)
    status = _STATUS[solver.solve(ctx.model)]

    has_value = bool(ctx.objective_terms) and status in (SolveStatus.OPTIMAL, SolveStatus.FEASIBLE)
    return RunResult(
        solver=solver,
        status=status,
        solve_time_seconds=solver.wall_time,
        objective_value=solver.objective_value if has_value else None,
        best_bound=solver.best_objective_bound if has_value else None,
    )
