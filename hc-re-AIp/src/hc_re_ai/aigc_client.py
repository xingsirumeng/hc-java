"""
AIGC text detection client backed by ZeroTrue (zerotrue-sdk).
"""

from zerotrue import ZeroTrue


class AigcClient:
    """Detect AI-generated content via ZeroTrue API."""

    def __init__(self, api_key: str):
        if not api_key:
            raise ValueError("ZEROTRUE_API_KEY is required")

        self._api_key = api_key
        self._client = ZeroTrue(api_key=api_key)

    # ------------------------------------------------------------------
    # public API
    # ------------------------------------------------------------------

    def detect(self, text: str) -> dict:
        """Detect AIGC probability for *text*.

        Returns a structured result::

            {
                "aigc_score": 85,          # 0-100, 越高越可能是AI生成
                "suggestion": "Review",     # Pass / Review / Block
                "label": "Aigc",
                "detail_results": [...],
                "request_id": "...",
                "raw": {...}               # ZeroTrue 完整原始响应
            }

        On error returns ``{"error": ..., "details": ...}``.
        """
        if not text:
            return {"error": "empty text", "details": "text must not be empty"}

        try:
            result = self._client.checks.create_and_wait({
                "input": {"type": "text", "value": text},
                "isPrivateScan": False,
            })
        except Exception as exc:
            return {"error": "request failed", "details": str(exc)}

        # Extract AI probability from ZeroTrue response
        ai_probability = result.get("ai_probability", 0)
        score = int(round(ai_probability * 100))

        # Map score to suggestion
        if score >= 80:
            suggestion = "Block"
        elif score >= 50:
            suggestion = "Review"
        else:
            suggestion = "Pass"

        return {
            "aigc_score": score,
            "suggestion": suggestion,
            "label": "Aigc" if score >= 50 else "Normal",
            "detail_results": [
                {
                    "Label": "Aigc",
                    "Score": score,
                    "Suggestion": suggestion,
                }
            ],
            "request_id": result.get("id", ""),
            "raw": result,
        }
