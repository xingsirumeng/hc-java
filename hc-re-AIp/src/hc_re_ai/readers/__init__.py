"""
文件读取器模块
每种文件类型对应一个读取器, 负责从文件中提取文字

后续 C++ pybind11 对应:
- 每个 Reader 对应 C++ 中的一个类
- pybind11 将 C++ 类绑定为 Python 可调用的接口
"""

from .base import BaseReader
from .txt_reader import TxtReader
from .docx_reader import DocxReader
from .image_reader import ImageReader

__all__ = ["BaseReader", "TxtReader", "DocxReader", "ImageReader"]
