"""
FastAPI Application for Text Processing

This FastAPI application provides endpoints for various text processing tasks, including YouTube video captions retrieval,
Google searches, and running text transformations using Hugging Face Transformers.

Endpoints:
- /ping: Check the status of the daemon.
- /app/youtube/cc/{video_id}: Retrieve YouTube video captions.
- /app/google/ss/: Perform a Google search.
- /: Get information about available routes and classifications cache keys.
- /known: Get information about known URIs.
- /transformers/pipeline/{task}: Run text transformations using Hugging Face Transformers.

Classifications Cache:
- A cache (_classifications_cache) is used to store preloaded classification models.

Model Initialization and Transformation:
- The transform endpoint initializes a classification model and performs a transformation on the provided input.

Running the Application:
- The application can be run using `uvicorn.run` with specified host, port, SSL key, SSL certificate, and worker count.

Usage:
    python main.py

Dependencies:
- FastAPI
- Pydantic
- YouTube Transcript API
- Googlesearch Python library
- Hugging Face Transformers
- uvicorn

Note: Ensure that the required dependencies are installed before running the application.

Author: Vladislav Maslow
"""

from fastapi import FastAPI
from pydantic import BaseModel
from starlette.responses import JSONResponse

from preload_models import preload_models

_classifications_cache = {

}


class ValueWrapped(BaseModel):
    value: str


app = FastAPI()


@app.get("/ping")
def ping():
    """
    Check the status of the daemon.

    Returns:
        JSONResponse: Response indicating the status of the daemon.
    """
    return (JSONResponse({
        "daemon": True,
        "ok": True,
    }, 200))


@app.get("/app/youtube/cc/{video_id}")
def cc(video_id: str):
    """
    Retrieve YouTube video captions.

    Args:
        video_id (str): YouTube video ID.

    Returns:
        JSONResponse: Transcripts of the specified YouTube video.
    """
    from youtube_transcript_api import YouTubeTranscriptApi

    return JSONResponse(YouTubeTranscriptApi.get_transcript(video_id), 200)


@app.get("/app/google/ss/")
def cc(q: str):
    """
    Perform a Google search.

    Args:
        q (str): Query string for the Google search.

    Returns:
        JSONResponse: Results of the Google search.
    """
    from googlesearch import search

    return JSONResponse(search(q, advanced=True, num_results=20, lang='en'), 200)


@app.get("/")
def index():
    """
    Get information about available routes and classifications cache keys.

    Returns:
       JSONResponse: Information about routes and cache keys.
    """
    url_list = [{"path": route.path, "name": route.name} for route in app.routes]

    return JSONResponse({
        "daemon": True,
        "uri": url_list,
        "_classifications_cache_keys": list(_classifications_cache.keys())
    }, 200)


@app.get("/known")
def known():
    """
    Get information about known URIs.

    Returns:
        JSONResponse: Information about known URIs.
    """
    return JSONResponse({
        "uri": list(preload_models),
    }, 200)


@app.post("/transformers/pipeline/{task}")
def transform(task: str, model: str, value: ValueWrapped):
    """
    Run text transformations using Hugging Face Transformers.

    Args:
       task (str): The task to perform.
       model (str): The model to use for the transformation.
       value (ValueWrapped): Input data for the transformation.

    Returns:
       JSONResponse: Result of the transformation, including classification and initialization times.
    """
    import time, os

    from transformers import pipeline

    _start_init = time.time()

    if task not in _classifications_cache:
        _classifications_cache[task] = pipeline(
            task,
            model=model
        )

    _classifier = _classifications_cache[task]

    _start_classifier = time.time()
    _output = _classifier(value.value)
    _end = time.time()

    return JSONResponse(
        {
            "task": task,
            "model": model,
            "output": _output,
            "classification_time": _end - _start_classifier,
            "init_time": _end - _start_init,
        },
        200
    )


if __name__ == "__main__":
    import uvicorn, os

    uvicorn.run(
        "main:app",
        host=str(os.getenv("FASTAPI_HOST")) or 'localhost',
        port=int(os.getenv("FASTAPI_PORT") or '9911') or 9911,
        reload=False,
        ssl_keyfile='./RootCA.key',
        ssl_certfile='./RootCA.crt',
        workers=int(os.getenv("WORK_COUNT") or '1') or 1
    )
