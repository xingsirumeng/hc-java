"""
文件读取器基类

定义统一的接口, 后续 C++ pybind11 绑定的类也应遵循此接口:
  class IReader {
  public:
      virtual std::string read(const std::string& filePath) = 0;
      virtual ~IReader() = default;
  };
"""

from abc import ABC, abstractmethod


class BaseReader(ABC):
    """文件读取器抽象基类"""

    @abstractmethod
    def read(self, file_path: str) -> str:
        """
        从文件中提取全部文字内容

        Args:
            file_path: 文件路径

        Returns:
            str: 提取的文字内容
        """
        pass

    @property
    @abstractmethod
    def supported_extensions(self) -> list:
        """返回支持的文件扩展名列表, 如 ['.txt']"""
        pass
