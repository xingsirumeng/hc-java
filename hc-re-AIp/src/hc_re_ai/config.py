"""
Configuration helpers for OCR and the HTTP service.
"""

import os
from pathlib import Path
from typing import Dict, Optional, Sequence

from dotenv import dotenv_values


DEFAULT_OCR_LANG_LIST = ["ch_sim", "en"]
DEFAULT_HTTP_HOST = "0.0.0.0"
DEFAULT_HTTP_PORT = 8000
DEFAULT_ZEROTRUE_API_KEY = ""


def _parse_bool(value, default: bool) -> bool:
    if value in (None, ""):
        return default
    return str(value).strip().lower() in {"1", "true", "yes", "on"}


def _parse_lang_list(value) -> list:
    if value in (None, ""):
        return list(DEFAULT_OCR_LANG_LIST)
    if isinstance(value, (list, tuple)):
        return [str(item).strip() for item in value if str(item).strip()]
    return [item.strip() for item in str(value).split(",") if item.strip()]


def _get_first_value(env_values: Dict[str, str], keys: Sequence[str]):
    for key in keys:
        value = os.environ.get(key)
        if value not in (None, ""):
            return value
    for key in keys:
        value = env_values.get(key)
        if value not in (None, ""):
            return value
    return None


def load_config(env_file: Optional[str] = None) -> dict:
    if env_file is None:
        env_file = Path(__file__).resolve().parents[2] / "api-data.env"

    env_path = Path(env_file)
    env_values = {}
    if env_path.exists():
        env_values = {
            key: value
            for key, value in dotenv_values(str(env_path)).items()
            if value is not None
        }

    lang_list = _parse_lang_list(
        _get_first_value(env_values, ("HC_RE_AI_OCR_LANG_LIST", "OCR_LANG_LIST"))
    )
    gpu = _parse_bool(
        _get_first_value(env_values, ("HC_RE_AI_OCR_GPU", "OCR_GPU")),
        default=False,
    )
    model_storage_directory = _get_first_value(
        env_values,
        ("HC_RE_AI_OCR_MODEL_STORAGE_DIR", "OCR_MODEL_STORAGE_DIR"),
    ) or ""
    download_enabled = _parse_bool(
        _get_first_value(
            env_values,
            ("HC_RE_AI_OCR_DOWNLOAD_ENABLED", "OCR_DOWNLOAD_ENABLED"),
        ),
        default=True,
    )
    http_host = _get_first_value(env_values, ("HC_RE_AI_HTTP_HOST", "HTTP_HOST"))
    http_port = _get_first_value(env_values, ("HC_RE_AI_HTTP_PORT", "HTTP_PORT"))

    zerotrue_api_key = _get_first_value(
        env_values, ("ZEROTRUE_API_KEY",)
    ) or DEFAULT_ZEROTRUE_API_KEY

    deepseek_api_key = _get_first_value(
        env_values, ("DEEPSEEK_API_KEY",)
    ) or ""
    grader_model = _get_first_value(
        env_values, ("GRADER_MODEL",)
    ) or "deepseek-chat"

    baidu_api_key = _get_first_value(
        env_values, ("BAIDU_API_KEY",)
    ) or ""
    baidu_secret_key = _get_first_value(
        env_values, ("BAIDU_SECRET_KEY",)
    ) or ""

    return {
        "OCR_LANG_LIST": lang_list,
        "OCR_GPU": gpu,
        "OCR_MODEL_STORAGE_DIR": model_storage_directory,
        "OCR_DOWNLOAD_ENABLED": download_enabled,
        "HTTP_HOST": http_host or DEFAULT_HTTP_HOST,
        "HTTP_PORT": int(http_port or DEFAULT_HTTP_PORT),
        "ZEROTRUE_API_KEY": zerotrue_api_key,
        "DEEPSEEK_API_KEY": deepseek_api_key,
        "GRADER_MODEL": grader_model,
        "BAIDU_API_KEY": baidu_api_key,
        "BAIDU_SECRET_KEY": baidu_secret_key,
    }


def set_config(
    ocr_lang_list: Optional[Sequence[str]] = None,
    ocr_gpu: Optional[bool] = None,
    ocr_model_storage_directory: Optional[str] = None,
    ocr_download_enabled: Optional[bool] = None,
    http_host: Optional[str] = None,
    http_port: Optional[int] = None,
):
    if ocr_lang_list is not None:
        os.environ["HC_RE_AI_OCR_LANG_LIST"] = ",".join(
            str(item).strip() for item in ocr_lang_list if str(item).strip()
        )
    if ocr_gpu is not None:
        os.environ["HC_RE_AI_OCR_GPU"] = "true" if ocr_gpu else "false"
    if ocr_model_storage_directory is not None:
        os.environ["HC_RE_AI_OCR_MODEL_STORAGE_DIR"] = ocr_model_storage_directory
    if ocr_download_enabled is not None:
        os.environ["HC_RE_AI_OCR_DOWNLOAD_ENABLED"] = (
            "true" if ocr_download_enabled else "false"
        )
    if http_host is not None:
        os.environ["HC_RE_AI_HTTP_HOST"] = http_host
    if http_port is not None:
        os.environ["HC_RE_AI_HTTP_PORT"] = str(http_port)
