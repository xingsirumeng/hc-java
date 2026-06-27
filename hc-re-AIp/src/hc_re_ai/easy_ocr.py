"""
EasyOCR client wrapper.
"""

from pathlib import Path
from threading import Lock
from typing import Dict, List, Optional, Sequence


DEFAULT_LANG_LIST = ("ch_sim", "en")


class EasyOcrClient:
    """Lazy EasyOCR wrapper used by the image reader."""

    def __init__(
        self,
        lang_list: Optional[Sequence[str]] = None,
        gpu: bool = False,
        model_storage_directory: Optional[str] = None,
        download_enabled: bool = True,
        recog_network: str = "standard",
    ):
        self._lang_list = list(lang_list or DEFAULT_LANG_LIST)
        self._gpu = gpu
        self._model_storage_directory = model_storage_directory
        self._download_enabled = download_enabled
        self._recog_network = recog_network
        self._reader = None
        self._lock = Lock()

    @property
    def lang_list(self) -> List[str]:
        return list(self._lang_list)

    def _build_reader(self):
        try:
            import easyocr
        except ImportError as exc:
            raise ImportError(
                "easyocr is not installed. Run `pip install easyocr` first."
            ) from exc

        kwargs = {
            "gpu": self._gpu,
            "download_enabled": self._download_enabled,
            "recog_network": self._recog_network,
        }
        if self._model_storage_directory:
            kwargs["model_storage_directory"] = self._model_storage_directory

        return easyocr.Reader(self._lang_list, **kwargs)

    def _ensure_reader(self):
        with self._lock:
            if self._reader is None:
                self._reader = self._build_reader()
            return self._reader

    @staticmethod
    def _normalize_result(result) -> Dict[str, object]:
        bbox, text, confidence = result
        xs = [point[0] for point in bbox]
        ys = [point[1] for point in bbox]

        left = int(round(min(xs)))
        top = int(round(min(ys)))
        right = int(round(max(xs)))
        bottom = int(round(max(ys)))

        return {
            "words": text,
            "location": {
                "left": left,
                "top": top,
                "width": max(0, right - left),
                "height": max(0, bottom - top),
            },
            "confidence": float(confidence),
            "polygon": [
                [int(round(point[0])), int(round(point[1]))] for point in bbox
            ],
        }

    def recognize(self, image_path: str) -> List[Dict[str, object]]:
        image_path = str(Path(image_path))
        reader = self._ensure_reader()

        with self._lock:
            results = reader.readtext(image_path, detail=1, paragraph=False)

        return [self._normalize_result(item) for item in results]

    def recognize_text_only(self, image_path: str) -> str:
        words_result = self.recognize(image_path)
        return "\n".join(item["words"] for item in words_result)
