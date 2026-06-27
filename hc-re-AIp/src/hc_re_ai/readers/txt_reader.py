"""
纯文本文件读取器 (.txt, .md, .csv 等)
"""

from .base import BaseReader


class TxtReader(BaseReader):
    """读取纯文本文件, 自动检测编码"""

    SUPPORTED = {".txt", ".md", ".csv", ".log", ".json", ".xml",
                 ".py", ".cpp", ".h", ".hpp", ".c", ".java", ".js", ".ts",
                 ".html", ".css", ".yaml", ".yml", ".toml", ".ini", ".cfg"}

    @property
    def supported_extensions(self) -> list:
        return sorted(self.SUPPORTED)

    def read(self, file_path: str) -> str:
        """
        读取文本文件内容, 自动尝试多种编码

        尝试顺序: UTF-8 -> GBK -> GB18030 -> Latin-1 (兜底)
        """
        encodings = ["utf-8", "gbk", "gb18030", "latin-1"]

        raw = None
        for enc in encodings:
            try:
                with open(file_path, "r", encoding=enc) as f:
                    return f.read()
            except UnicodeDecodeError:
                continue

        # 最终兜底: 二进制读取 + errors='replace'
        with open(file_path, "r", encoding="utf-8", errors="replace") as f:
            return f.read()
