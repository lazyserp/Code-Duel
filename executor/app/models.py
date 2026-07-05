from pydantic import BaseModel
from typing import Optional


class Submission(BaseModel):
    source_code: str
    language: str
    stdin: str = ""
    expected_output: str = ""
    time_limit: int = 10
    memory_limit: int = 256


class ExecuteRequest(BaseModel):
    submissions: list[Submission]


class SubmissionResult(BaseModel):
    status: str
    time: float
    memory: int = 0
    stderr: Optional[str] = None
    compile_output: Optional[str] = None


class ExecuteResponse(BaseModel):
    results: list[SubmissionResult]
