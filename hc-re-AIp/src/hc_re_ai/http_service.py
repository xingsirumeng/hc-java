"""
HTTP service entrypoints for Java or other backend callers.
"""

import argparse
from typing import Any, Dict, List, Optional

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, Field

from .config import load_config
from .job_processor import JobProcessor
from .aigc_client import AigcClient
from .grader_client import GraderClient


class ProcessPathRequest(BaseModel):
    file_path: str
    with_location: bool = False


class BatchProcessRequest(BaseModel):
    file_paths: List[str] = Field(default_factory=list)
    with_location: bool = False


class AigcDetectRequest(BaseModel):
    text: str


class GradeRequest(BaseModel):
    assignment: str = Field(description="作业要求/问题描述")
    text: str = Field(description="学生提交的文本内容")


class ProcessResponse(BaseModel):
    file_path: str
    text: str = ""
    success: bool = False
    error: str = ""
    file_type: str = ""
    words_result: List[Dict[str, Any]] = Field(default_factory=list)


def _build_processor() -> JobProcessor:
    config = load_config()
    return JobProcessor(
        baidu_api_key=config.get("BAIDU_API_KEY", ""),
        baidu_secret_key=config.get("BAIDU_SECRET_KEY", ""),
        ocr_lang_list=config["OCR_LANG_LIST"],
        ocr_gpu=config["OCR_GPU"],
        ocr_model_storage_directory=config["OCR_MODEL_STORAGE_DIR"] or None,
        ocr_download_enabled=config["OCR_DOWNLOAD_ENABLED"],
    )


def _build_aigc_client() -> Optional[AigcClient]:
    config = load_config()
    api_key = config.get("ZEROTRUE_API_KEY", "")
    if not api_key:
        return None
    return AigcClient(api_key=api_key)


def _build_grader_client() -> Optional[GraderClient]:
    config = load_config()
    api_key = config.get("DEEPSEEK_API_KEY", "")
    if not api_key:
        return None
    return GraderClient(
        api_key=api_key,
        model=config.get("GRADER_MODEL", "deepseek-chat"),
    )


def create_app(processor: Optional[JobProcessor] = None) -> FastAPI:
    config = load_config()
    processor = processor or _build_processor()
    aigc_client = _build_aigc_client()
    grader_client = _build_grader_client()

    app = FastAPI(
        title="hc_re_ai HTTP Service",
        version="0.4.0",
        description="Text extraction + AIGC detection + AI grading.",
    )

    @app.get("/health")
    async def health() -> Dict[str, Any]:
        return {
            "status": "ok",
            "ocr_engine": processor.ocr_engine,
            "ocr_lang_list": config["OCR_LANG_LIST"],
            "ocr_gpu": config["OCR_GPU"],
            "aigc_available": aigc_client is not None,
            "grader_available": grader_client is not None,
        }

    @app.post("/api/v1/aigc/detect")
    async def aigc_detect(request: AigcDetectRequest) -> Dict[str, Any]:
        if aigc_client is None:
            raise HTTPException(
                status_code=503,
                detail="AIGC detection is not configured (missing ZEROTRUE_API_KEY)",
            )
        return aigc_client.detect(request.text)

    @app.post("/api/v1/aigc/detect/upload")
    async def aigc_detect_upload(
        file: UploadFile = File(...),
    ) -> Dict[str, Any]:
        if aigc_client is None:
            raise HTTPException(
                status_code=503,
                detail="AIGC detection is not configured (missing ZEROTRUE_API_KEY)",
            )
        if not file.filename:
            raise HTTPException(status_code=400, detail="filename is required")

        content = await file.read()
        if not content:
            raise HTTPException(status_code=400, detail="uploaded file is empty")

        # extract text from the uploaded file via JobProcessor
        extract_result = processor.process_bytes(
            file_name=file.filename,
            content=content,
        )
        if not extract_result.get("success"):
            return {
                "error": "text extraction failed",
                "details": extract_result.get("error", "unknown"),
                "file_name": file.filename,
                "file_type": extract_result.get("file_type", ""),
            }

        text = extract_result.get("text", "")
        if not text.strip():
            return {
                "error": "no text extracted",
                "details": "the file contained no readable text",
                "file_name": file.filename,
                "file_type": extract_result.get("file_type", ""),
            }

        # run AIGC detection on the extracted text
        aigc_result = aigc_client.detect(text)

        return {
            "file_name": file.filename,
            "file_type": extract_result.get("file_type", ""),
            "extracted_text": text,
            "aigc_result": aigc_result,
        }

    # ------------------------------------------------------------------
    # AI grading endpoints
    # ------------------------------------------------------------------

    @app.post("/api/v1/grade")
    async def grade_text(request: GradeRequest) -> Dict[str, Any]:
        if grader_client is None:
            raise HTTPException(
                status_code=503,
                detail="Grading is not configured (missing DEEPSEEK_API_KEY)",
            )
        return grader_client.grade(request.assignment, request.text)

    @app.post("/api/v1/grade/upload")
    async def grade_upload(
        assignment: str = Form(..., description="作业要求/问题描述"),
        file: UploadFile = File(...),
    ) -> Dict[str, Any]:
        if grader_client is None:
            raise HTTPException(
                status_code=503,
                detail="Grading is not configured (missing DEEPSEEK_API_KEY)",
            )
        if not file.filename:
            raise HTTPException(status_code=400, detail="filename is required")

        content = await file.read()
        if not content:
            raise HTTPException(status_code=400, detail="uploaded file is empty")

        # extract text from the uploaded file
        extract_result = processor.process_bytes(
            file_name=file.filename,
            content=content,
        )
        if not extract_result.get("success"):
            return {
                "error": "text extraction failed",
                "details": extract_result.get("error", "unknown"),
                "file_name": file.filename,
                "file_type": extract_result.get("file_type", ""),
            }

        text = extract_result.get("text", "")
        if not text.strip():
            return {
                "error": "no text extracted",
                "details": "the file contained no readable text",
                "file_name": file.filename,
                "file_type": extract_result.get("file_type", ""),
            }

        result = grader_client.grade(assignment, text)
        return {
            "file_name": file.filename,
            "file_type": extract_result.get("file_type", ""),
            "extracted_text": text,
            **result,
        }

    @app.post("/api/v1/process/path", response_model=ProcessResponse)
    async def process_path(request: ProcessPathRequest) -> Dict[str, Any]:
        return processor.process_to_dict(
            request.file_path,
            with_location=request.with_location,
        )

    @app.post("/api/v1/process/batch", response_model=List[ProcessResponse])
    async def process_batch(request: BatchProcessRequest) -> List[Dict[str, Any]]:
        return [
            processor.process_to_dict(
                file_path,
                with_location=request.with_location,
            )
            for file_path in request.file_paths
        ]

    @app.post("/api/v1/process/upload", response_model=ProcessResponse)
    async def process_upload(
        file: UploadFile = File(...),
        with_location: bool = Form(False),
    ) -> Dict[str, Any]:
        if not file.filename:
            raise HTTPException(status_code=400, detail="filename is required")

        content = await file.read()
        if not content:
            raise HTTPException(status_code=400, detail="uploaded file is empty")

        return processor.process_bytes(
            file_name=file.filename,
            content=content,
            with_location=with_location,
        )

    return app


def main():
    try:
        import uvicorn
    except ImportError as exc:
        raise ImportError(
            "uvicorn is not installed. Run `pip install uvicorn fastapi python-multipart`."
        ) from exc

    config = load_config()

    parser = argparse.ArgumentParser(description="Run hc_re_ai HTTP service.")
    parser.add_argument("--host", default=config["HTTP_HOST"])
    parser.add_argument("--port", type=int, default=config["HTTP_PORT"])
    args = parser.parse_args()

    uvicorn.run(create_app(), host=args.host, port=args.port)


if __name__ == "__main__":
    main()
