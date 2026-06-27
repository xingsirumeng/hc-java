"""
AI grading client backed by DeepSeek Chat API (OpenAI-compatible).

Takes an assignment description + student submission, returns a score (0-100)
and Chinese commentary via the DeepSeek Chat completions endpoint.
"""

import json
import urllib.request
from typing import Optional


# prompt that instructs the model to return structured JSON
GRADER_SYSTEM_PROMPT = """你是一位严格但公正的中文作业评阅专家。你的任务是：

1. 仔细阅读「作业要求」和「学生提交内容」
2. 根据作业要求评估学生提交的质量
3. 给出 0-100 的分数和一段中文评语

评分标准：
- 90-100：优秀，完全满足要求，有深度和创见
- 80-89：良好，满足大部分要求，思路清晰
- 70-79：中等，基本满足要求但有明显不足
- 60-69：及格，勉强满足要求，存在较多问题
- 0-59：不及格，未达到基本要求

评语要求：
- 100字以上
- 先指出优点，再指出不足
- 给出具体的改进建议
- 语气客观、建设性

请严格按照以下 JSON 格式输出，不要输出其他内容：
{"score": 85, "commentary": "评语内容..."}"""


class GraderClient:
    """Grade student submissions against an assignment prompt via DeepSeek."""

    def __init__(
        self,
        api_key: str,
        model: str = "deepseek-chat",
        base_url: str = "https://api.deepseek.com/v1",
    ):
        if not api_key:
            raise ValueError("DEEPSEEK_API_KEY is required")
        self._api_key = api_key
        self._model = model
        self._url = f"{base_url}/chat/completions"

    # ------------------------------------------------------------------
    # public API
    # ------------------------------------------------------------------

    def grade(self, assignment: str, submission: str) -> dict:
        """Evaluate *submission* against *assignment* and return score + commentary.

        Returns::

            {
                "score": 85,
                "commentary": "...",
                "model": "deepseek-chat",
                "raw": {...}
            }

        On error returns ``{"error": ..., "details": ...}``.
        """
        if not assignment.strip():
            return {"error": "empty assignment", "details": "assignment must not be empty"}
        if not submission.strip():
            return {"error": "empty submission", "details": "submission must not be empty"}

        messages = [
            {"role": "system", "content": GRADER_SYSTEM_PROMPT},
            {
                "role": "user",
                "content": (
                    f"【作业要求】\n{assignment}\n\n"
                    f"【学生提交】\n{submission}"
                ),
            },
        ]

        payload = json.dumps(
            {
                "model": self._model,
                "messages": messages,
                "temperature": 0.3,
                "max_tokens": 1024,
            }
        )

        try:
            body = self._call(payload)
            data = json.loads(body)
        except (urllib.error.URLError, ValueError) as exc:
            return {"error": "request failed", "details": str(exc)}

        # check for API-level errors
        if "error" in data:
            err = data["error"]
            return {
                "error": err.get("type", "api_error"),
                "details": err.get("message", str(err)),
            }

        # extract assistant message
        choices = data.get("choices", [])
        if not choices:
            return {"error": "no response", "details": "API returned empty choices"}

        content = choices[0].get("message", {}).get("content", "")

        # parse the JSON from model output
        parsed = self._parse_output(content)
        if parsed is None:
            # model didn't return valid JSON; return raw content
            return {
                "score": 0,
                "commentary": content,
                "model": data.get("model", self._model),
                "parse_error": True,
                "raw": data,
            }

        return {
            "score": parsed.get("score", 0),
            "commentary": parsed.get("commentary", ""),
            "model": data.get("model", self._model),
            "raw": data,
        }

    # ------------------------------------------------------------------
    # internals
    # ------------------------------------------------------------------

    def _call(self, payload: str) -> str:
        req = urllib.request.Request(
            self._url,
            data=payload.encode("utf-8"),
            headers={
                "Authorization": f"Bearer {self._api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=60) as resp:
            return resp.read().decode("utf-8")

    @staticmethod
    def _parse_output(content: str) -> Optional[dict]:
        """Try to extract a JSON object from the model output."""
        # try direct parse first
        try:
            return json.loads(content)
        except json.JSONDecodeError:
            pass

        # try to find JSON inside markdown code fences
        import re

        m = re.search(r"\{[^{}]*\"score\"[^{}]*\"commentary\"[^{}]*\}", content, re.DOTALL)
        if m:
            try:
                return json.loads(m.group())
            except json.JSONDecodeError:
                pass

        return None
