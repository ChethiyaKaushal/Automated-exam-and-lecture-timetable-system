import copy
import json
from pathlib import Path

from fastapi.testclient import TestClient

from app.main import app, get_solver
from app.schemas import SolverOutput

FIXTURES = Path(__file__).parent / "fixtures"
SMALL_INPUT = json.loads((FIXTURES / "small_input.json").read_text(encoding="utf-8"))
SMALL_OUTPUT = json.loads((FIXTURES / "small_output.json").read_text(encoding="utf-8"))
GENERATE = "/v1/exam-timetables/generate"

client = TestClient(app)


def body(**options) -> dict:
    data = {"input": copy.deepcopy(SMALL_INPUT)}
    if options:
        data["options"] = options
    return data


def use_solver(fn) -> None:
    app.dependency_overrides[get_solver] = lambda: fn


def teardown_function() -> None:
    app.dependency_overrides.clear()


def test_health_reports_status():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert {"solver", "geminiConfigured"} <= data.keys()


def test_generate_returns_the_solver_output():
    expected = SolverOutput.model_validate(SMALL_OUTPUT)
    calls = []

    def fake_solver(problem, time_limit):
        calls.append((problem.period_id, time_limit))
        return expected

    use_solver(fake_solver)
    response = client.post(GENERATE, json=body(timeLimitSeconds=5))

    assert response.status_code == 200, response.text
    output = response.json()["output"]
    assert output["status"] == "OPTIMAL"
    assert SolverOutput.model_validate(output) == expected
    assert calls == [(SMALL_INPUT["periodId"], 5.0)]


def test_default_time_limit_comes_from_settings(monkeypatch):
    monkeypatch.setenv("SOLVER_TIME_LIMIT_SECONDS", "42")
    calls = []
    use_solver(lambda problem, time_limit: calls.append(time_limit) or SolverOutput.model_validate(SMALL_OUTPUT))

    client.post(GENERATE, json=body())

    assert calls == [42.0]


def test_without_a_solver_the_admin_gets_a_reason():
    response = client.post(GENERATE, json=body())

    assert response.status_code == 200
    output = response.json()["output"]
    assert output["status"] == "MODEL_INVALID"
    assert output["diagnostics"][0]["code"] == "INTERNAL_ERROR"
    assert output["assignments"] == []


def test_solver_crash_becomes_an_internal_error():
    def broken_solver(problem, time_limit):
        raise RuntimeError("boom")

    use_solver(broken_solver)
    output = client.post(GENERATE, json=body()).json()["output"]

    assert output["status"] == "MODEL_INVALID"
    assert "boom" in output["diagnostics"][0]["message"]


def test_invalid_input_is_rejected_before_solving():
    called = []
    use_solver(lambda problem, time_limit: called.append(1))
    data = body()
    data["input"]["exams"][0]["studentCount"] += 1  # no longer matches its groups

    response = client.post(GENERATE, json=data)

    assert response.status_code == 422
    assert called == []


def test_unknown_option_is_rejected():
    response = client.post(GENERATE, json=body(colour="blue"))
    assert response.status_code == 422

"""
What the 7 tests check:

1./health answers UP.
2./generate returns exactly what the solver produced.
3.With no time limit in the request, the default comes from settings.
4.Without the real solver, the admin gets a clear "not connected" reason.
5.A solver crash turns into a readable error instead of breaking the server.
6.Bad input is rejected (422) before the solver runs.
7.Unknown options are rejected.
"""