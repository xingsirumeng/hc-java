"""
Core file processing entrypoints.
"""

import os
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence

from .readers import DocxReader, ImageReader, TxtReader


IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".tiff", ".tif", ".gif", ".webp"}
TEXT_EXTENSIONS = {
    ".txt",
    ".md",
    ".csv",
    ".log",
    ".json",
    ".xml",
    ".py",
    ".cpp",
    ".h",
    ".hpp",
    ".c",
    ".java",
    ".js",
    ".ts",
    ".html",
    ".css",
    ".yaml",
    ".yml",
    ".toml",
    ".ini",
    ".cfg",
}
DOCX_EXTENSIONS = {".docx"}


@dataclass
class JobResult:
    file_path: str
    text: str = ""
    success: bool = False
    error: str = ""
    file_type: str = ""


def detect_file_type(file_path: str) -> str:
    ext = Path(file_path).suffix.lower()
    if ext in TEXT_EXTENSIONS:
        return "txt"
    if ext in DOCX_EXTENSIONS:
        return "docx"
    if ext in IMAGE_EXTENSIONS:
        return "image"
    return "unknown"


def job_result_to_dict(result: JobResult) -> Dict[str, Any]:
    return {
        "file_path": result.file_path,
        "text": result.text,
        "success": result.success,
        "error": result.error,
        "file_type": result.file_type,
        "words_result": [],
    }


class JobProcessor:
    def __init__(
        self,
        baidu_api_key: str = "",
        baidu_secret_key: str = "",
        ocr_lang_list: Optional[Sequence[str]] = None,
        ocr_gpu: bool = False,
        ocr_model_storage_directory: Optional[str] = None,
        ocr_download_enabled: bool = True,
        ocr_recog_network: str = "standard",
    ):
        from .baidu_ocr import BaiduOcrClient

        baidu_client = None
        if baidu_api_key and baidu_secret_key:
            baidu_client = BaiduOcrClient(
                api_key=baidu_api_key,
                secret_key=baidu_secret_key,
            )

        self._txt_reader = TxtReader()
        self._docx_reader = DocxReader()
        self._image_reader = ImageReader(
            lang_list=ocr_lang_list,
            gpu=ocr_gpu,
            model_storage_directory=ocr_model_storage_directory,
            download_enabled=ocr_download_enabled,
            recog_network=ocr_recog_network,
            baidu_client=baidu_client,
        )

    def configure_ocr(
        self,
        ocr_lang_list: Optional[Sequence[str]] = None,
        ocr_gpu: bool = False,
        ocr_model_storage_directory: Optional[str] = None,
        ocr_download_enabled: bool = True,
        ocr_recog_network: str = "standard",
    ):
        self._image_reader.configure(
            lang_list=ocr_lang_list,
            gpu=ocr_gpu,
            model_storage_directory=ocr_model_storage_directory,
            download_enabled=ocr_download_enabled,
            recog_network=ocr_recog_network,
        )

    def set_ocr_credentials(self, api_key: str = "", secret_key: str = ""):
        self._image_reader.set_credentials(api_key=api_key, secret_key=secret_key)

    @property
    def ocr_engine(self) -> str:
        return self._image_reader.ocr_engine

    def process(self, file_path: str) -> JobResult:
        file_path = str(file_path)

        if not os.path.isfile(file_path):
            return JobResult(
                file_path=file_path,
                success=False,
                error="file does not exist: %s" % file_path,
            )

        file_type = detect_file_type(file_path)

        try:
            if file_type == "txt":
                text = self._txt_reader.read(file_path)
            elif file_type == "docx":
                text = self._docx_reader.read(file_path)
            elif file_type == "image":
                text = self._image_reader.read(file_path)
            else:
                try:
                    text = self._txt_reader.read(file_path)
                    file_type = "txt (fallback)"
                except Exception:
                    return JobResult(
                        file_path=file_path,
                        success=False,
                        error="unsupported file type: %s" % Path(file_path).suffix,
                    )

            return JobResult(
                file_path=file_path,
                text=text,
                success=True,
                file_type=file_type,
            )
        except Exception as exc:
            return JobResult(
                file_path=file_path,
                success=False,
                error=str(exc),
                file_type=file_type,
            )

    def process_batch(self, file_paths: List[str]) -> List[JobResult]:
        return [self.process(path) for path in file_paths]

    def process_to_dict(
        self,
        file_path: str,
        with_location: bool = False,
    ) -> Dict[str, Any]:
        file_type = detect_file_type(file_path)
        if with_location and file_type == "image":
            return self.process_image_with_location(file_path)
        return job_result_to_dict(self.process(file_path))

    def process_bytes(
        self,
        file_name: str,
        content: bytes,
        with_location: bool = False,
    ) -> Dict[str, Any]:
        suffix = Path(file_name).suffix or ".tmp"
        temp_path = ""

        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp_file:
            temp_file.write(content)
            temp_path = temp_file.name

        try:
            response = self.process_to_dict(temp_path, with_location=with_location)
            response["file_path"] = file_name
            return response
        finally:
            if temp_path and os.path.exists(temp_path):
                os.unlink(temp_path)

    def process_image_with_location(self, file_path: str) -> Dict[str, Any]:
        file_path = str(file_path)

        if not os.path.isfile(file_path):
            return {
                "file_path": file_path,
                "text": "",
                "success": False,
                "error": "file does not exist: %s" % file_path,
                "file_type": "image",
                "words_result": [],
            }

        file_type = detect_file_type(file_path)
        if file_type != "image":
            return {
                "file_path": file_path,
                "text": "",
                "success": False,
                "error": "not an image file: %s" % file_type,
                "file_type": file_type,
                "words_result": [],
            }

        try:
            words_result = self._image_reader.read_with_location(file_path)
            text = "\n".join(item["words"] for item in words_result)
            return {
                "file_path": file_path,
                "text": text,
                "success": True,
                "error": "",
                "file_type": "image",
                "words_result": words_result,
            }
        except Exception as exc:
            return {
                "file_path": file_path,
                "text": "",
                "success": False,
                "error": str(exc),
                "file_type": "image",
                "words_result": [],
            }
