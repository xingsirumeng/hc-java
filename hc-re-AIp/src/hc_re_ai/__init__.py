"""
hc_re_ai package exports.
"""

from .config import load_config, set_config
from .easy_ocr import EasyOcrClient
from .job_processor import JobProcessor, JobResult, detect_file_type
from .readers import DocxReader, ImageReader, TxtReader

__version__ = "0.2.0"
__all__ = [
    "JobProcessor",
    "JobResult",
    "detect_file_type",
    "load_config",
    "set_config",
    "EasyOcrClient",
    "TxtReader",
    "DocxReader",
    "ImageReader",
]
