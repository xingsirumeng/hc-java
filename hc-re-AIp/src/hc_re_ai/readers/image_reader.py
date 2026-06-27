"""
Image reader backed by Baidu OCR (primary) with EasyOCR fallback.
"""

import logging
from typing import Optional, Sequence

from .base import BaseReader
from ..easy_ocr import EasyOcrClient
from ..baidu_ocr import BaiduOcrClient

logger = logging.getLogger(__name__)


class ImageReader(BaseReader):
    SUPPORTED = {".png", ".jpg", ".jpeg", ".bmp", ".tiff", ".tif", ".gif", ".webp"}

    def __init__(
        self,
        lang_list: Optional[Sequence[str]] = None,
        gpu: bool = False,
        model_storage_directory: Optional[str] = None,
        download_enabled: bool = True,
        recog_network: str = "standard",
        baidu_client: Optional[BaiduOcrClient] = None,
    ):
        self._baidu = baidu_client
        self._easyocr = EasyOcrClient(
            lang_list=lang_list,
            gpu=gpu,
            model_storage_directory=model_storage_directory,
            download_enabled=download_enabled,
            recog_network=recog_network,
        )

    def configure(
        self,
        lang_list: Optional[Sequence[str]] = None,
        gpu: bool = False,
        model_storage_directory: Optional[str] = None,
        download_enabled: bool = True,
        recog_network: str = "standard",
    ):
        self._easyocr = EasyOcrClient(
            lang_list=lang_list,
            gpu=gpu,
            model_storage_directory=model_storage_directory,
            download_enabled=download_enabled,
            recog_network=recog_network,
        )

    def set_credentials(self, api_key: str = "", secret_key: str = ""):
        if api_key or secret_key:
            self._baidu = BaiduOcrClient(api_key=api_key, secret_key=secret_key)
        else:
            self._baidu = None

    @property
    def supported_extensions(self) -> list:
        return sorted(self.SUPPORTED)

    @property
    def ocr_engine(self) -> str:
        """Describe the current OCR engine stack."""
        parts = []
        if self._baidu and self._baidu.available:
            parts.append("baidu")
        parts.append("easyocr")
        return "+".join(parts)

    # ------------------------------------------------------------------
    # read / read_with_location
    # ------------------------------------------------------------------

    def read(self, file_path: str) -> str:
        """Extract text from image, trying Baidu first then EasyOCR."""
        if self._baidu and self._baidu.available:
            try:
                return self._baidu.recognize_text_only(file_path)
            except Exception as exc:
                logger.warning("Baidu OCR failed, falling back to EasyOCR: %s", exc)

        return self._easyocr.recognize_text_only(file_path)

    def read_with_location(self, file_path: str) -> list:
        """Extract text with location, trying Baidu first then EasyOCR."""
        if self._baidu and self._baidu.available:
            try:
                return self._baidu.recognize(file_path)
            except Exception as exc:
                logger.warning("Baidu OCR failed, falling back to EasyOCR: %s", exc)

        return self._easyocr.recognize(file_path)
