import asyncio
from fastapi import FastAPI, HTTPException
import docker

from app.models import ExecuteRequest, ExecuteResponse
from app.executor import execute, _client

app = FastAPI(title="CodeDuel Executor")

_RUNNER_IMAGES = {
    "java": "codeduel-runner-java",
    "python": "codeduel-runner-python",
    "cpp": "codeduel-runner-cpp",
}

# Max containers running simultaneously — prevents Docker daemon overload
_semaphore = asyncio.Semaphore(20)


@app.get("/health")
def health():
    runners = {}
    for lang, image in _RUNNER_IMAGES.items():
        try:
            _client.images.get(image)
            runners[lang] = True
        except docker.errors.ImageNotFound:
            runners[lang] = False
    return {"status": "ok", "runners": runners}


@app.post("/execute", response_model=ExecuteResponse)
async def execute_batch(request: ExecuteRequest):
    results = []
    for submission in request.submissions:
        try:
            async with _semaphore:
                result = await asyncio.to_thread(execute, submission)
        except docker.errors.ImageNotFound as e:
            raise HTTPException(status_code=500, detail=f"Runner image not found: {e}")
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))
        results.append(result)
    return ExecuteResponse(results=results)
