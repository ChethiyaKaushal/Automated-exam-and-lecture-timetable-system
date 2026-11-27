"""FastAPI entry point for the AI scheduling service."""

from fastapi import Depends, FastAPI

from app import solver_gateway
from app.config import Settings, load_settings
from app.schemas.api import GenerateRequest, GenerateResponse

app = FastAPI(title="Timetable AI service", version="0.1.0")


def get_settings() -> Settings:
    return load_settings()


def get_solver() -> solver_gateway.SolveFunction:
    return solver_gateway.solve


@app.get("/health")
def health(settings: Settings = Depends(get_settings)) -> dict:
    return {
        "status": "UP",
        "solver": solver_gateway.SOLVER_NAME,
        "geminiConfigured": settings.gemini_configured,
    }


# Plain `def`: FastAPI runs it in a worker thread, so a long solve does not block /health.
@app.post("/v1/exam-timetables/generate", response_model=GenerateResponse, response_model_by_alias=True)
def generate(
        request: GenerateRequest,
        settings: Settings = Depends(get_settings),
        solve: solver_gateway.SolveFunction = Depends(get_solver),
) -> GenerateResponse:
    time_limit = request.options.time_limit_seconds or settings.solver_time_limit_seconds
    output = solver_gateway.run(solve, request.input, time_limit)
    return GenerateResponse(solver=solver_gateway.SOLVER_NAME, output=output)