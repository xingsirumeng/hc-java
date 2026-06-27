"""
Baidu OCR API client (accurate_basic endpoint).

Uses OAuth 2.0 for access token; caches token for its validity period (~30 days).
"""

import base64
import time
from pathlib import Path
from threading import Lock
from typing import Any, Dict, List, Optional

import urllib.request
import urllib.parse
import json


TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic"
TOKEN_EXPIRY_MARGIN = 300  # refresh 5 minutes before actual expiry


class BaiduOcrClient:
    """Baidu OCR API client — same interface as EasyOcrClient for drop-in use."""

    def __init__(self, api_key: str = "", secret_key: str = ""):
        self._api_key = api_key
        self._secret_key = secret_key
        self._access_token: Optional[str] = None
        self._token_expiry: float = 0.0
        self._lock = Lock()

    @property
    def available(self) -> bool:
        """Baidu OCR is available when both api_key and secret_key are set."""
        return bool(self._api_key and self._secret_key)

    # ------------------------------------------------------------------
    # public API (mirrors EasyOcrClient)
    # ------------------------------------------------------------------

    def recognize(self, image_path: str) -> List[Dict[str, Any]]:
        """Run OCR on *image_path*, return list of dicts with
        words / location / confidence / polygon.
        """
        image_b64 = self._encode_image(image_path)
        result = self._call_ocr(image_b64)

        words_result: List[Dict[str, Any]] = result.get("words_result", [])

        normalized: List[Dict[str, Any]] = []
        for item in words_result:
            words = item.get("words", "")
            loc = item.get("location", {})
            left = int(round(loc.get("left", 0)))
            top = int(round(loc.get("top", 0)))
            width = int(round(loc.get("width", 0)))
            height = int(round(loc.get("height", 0)))

            normalized.append({
                "words": words,
                "location": {
                    "left": left,
                    "top": top,
                    "width": width,
                    "height": height,
                },
                "confidence": 1.0,
                "polygon": [
                    [left, top],
                    [left + width, top],
                    [left + width, top + height],
                    [left, top + height],
                ],
            })

        return normalized

    def recognize_text_only(self, image_path: str) -> str:
        """Run OCR on *image_path*, return plain text (lines joined by newline)."""
        words_result = self.recognize(image_path)
        return "\n".join(item["words"] for item in words_result)

    # ------------------------------------------------------------------
    # internals
    # ------------------------------------------------------------------

    def _ensure_token(self) -> str:
        with self._lock:
            if self._access_token and time.time() < self._token_expiry - TOKEN_EXPIRY_MARGIN:
                return self._access_token

            params = urllib.parse.urlencode({
                "grant_type": "client_credentials",
                "client_id": self._api_key,
                "client_secret": self._secret_key,
            })
            url = f"{TOKEN_URL}?{params}"
            req = urllib.request.Request(url, method="POST")
            with urllib.request.urlopen(req, timeout=30) as resp:
                data = json.loads(resp.read().decode("utf-8"))

            if "error" in data:
                raise RuntimeError(
                    f"Baidu OAuth error: {data.get('error')} — {data.get('error_description', '')}"
                )

            self._access_token = data["access_token"]
            self._token_expiry = time.time() + data.get("expires_in", 2592000)
            return self._access_token

    def _call_ocr(self, image_b64: str) -> Dict[str, Any]:
        token = self._ensure_token()
        payload = urllib.parse.urlencode({"image": image_b64}).encode("utf-8")
        url = f"{OCR_URL}?access_token={token}"

        req = urllib.request.Request(
            url,
            data=payload,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read().decode("utf-8"))

        if "error_code" in data:
            raise RuntimeError(
                f"Baidu OCR error {data.get('error_code')}: {data.get('error_msg', 'unknown')}"
            )

        return data

    @staticmethod
    def _encode_image(image_path: str) -> str:
        image_path = str(Path(image_path))
        with open(image_path, "rb") as f:
            return base64.b64encode(f.read()).decode("ascii")
