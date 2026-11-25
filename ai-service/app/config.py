"""Settings read from environment variables."""

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    solver_time_limit_seconds: float
    gemini_api_key: str
    gemini_model: str

    @property
    def gemini_configured(self) -> bool:
        return bool(self.gemini_api_key)


def load_settings() -> Settings:
    return Settings(
        solver_time_limit_seconds=float(os.environ.get("SOLVER_TIME_LIMIT_SECONDS", "60")),
        gemini_api_key=os.environ.get("GEMINI_API_KEY", "").strip(),
        gemini_model=os.environ.get("GEMINI_MODEL", "gemini-3.6-flash").strip(),
    )