"""The single place where the API calls the solver.
The CP-SAT solver (app/solver/, Nimmitha + Malsha) must provide:
    def solve(problem: SolverInput, time_limit_seconds: float) -> SolverOutput
When it is merged, replace `_solver_not_ready` below with that function and set SOLVER_NAME = "cp-sat".
Until then every request returns a valid MODEL_INVALID output that says the solver is not ready.
"""

import time
from collections.abc import Callable

from app.schemas import (
    Diagnostic,
    DiagnosticCode,
    DiagnosticSource,
    SolverInput,
    SolverOutput,
    SolverStats,
    SolveStatus,
)

SolveFunction = Callable[[SolverInput, float], SolverOutput]


def failure(problem: SolverInput, time_limit_seconds: float, elapsed: float, message: str) -> SolverOutput:
    """A contract-valid output for a solver that could not run."""
    return SolverOutput(
        status=SolveStatus.MODEL_INVALID,
        period_id=problem.period_id,
        diagnostics=[Diagnostic(code=DiagnosticCode.INTERNAL_ERROR, source=DiagnosticSource.SOLVER,
                                message=message)],
        stats=SolverStats(solve_time_seconds=elapsed, time_limit_seconds=time_limit_seconds),
    )


def _solver_not_ready(problem: SolverInput, time_limit_seconds: float) -> SolverOutput:
    return failure(problem, time_limit_seconds, 0.0,
                   "The CP-SAT solver is not connected yet. Generate with the Java engine for now.")


SOLVER_NAME = "not-ready"
solve: SolveFunction = _solver_not_ready


def run(solve_fn: SolveFunction, problem: SolverInput, time_limit_seconds: float) -> SolverOutput:
    """Call the solver and turn any crash into an INTERNAL_ERROR output, so the API always answers."""
    started = time.monotonic()
    try:
        return solve_fn(problem, time_limit_seconds)
    except Exception as error:  # a solver bug must not crash the service
        return failure(problem, time_limit_seconds, time.monotonic() - started,
                       f"The solver failed: {type(error).__name__}: {error}")