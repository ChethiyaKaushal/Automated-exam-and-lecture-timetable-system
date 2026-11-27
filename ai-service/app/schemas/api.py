"""HTTP request and response bodies. They wrap the solver contract in solver_input.py and solver_output.py."""

from pydantic import Field

from app.schemas.solver_input import CamelModel, SolverInput
from app.schemas.solver_output import SolverOutput


class GenerateOptions(CamelModel):
    # None uses SOLVER_TIME_LIMIT_SECONDS.
    time_limit_seconds: float | None = Field(default=None, gt=0, le=600)
    explain: bool = True


class GenerateRequest(CamelModel):
    input: SolverInput
    options: GenerateOptions = Field(default_factory=GenerateOptions)


class GenerateResponse(CamelModel):
    # Which solver produced the output, for example "cp-sat".
    solver: str
    output: SolverOutput
    # Plain-language summary for the admin. Filled by the LangGraph workflow (Day 5) and Gemini (Day 7).
    explanation: str | None = None