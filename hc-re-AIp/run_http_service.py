"""
Convenience launcher for the local HTTP service.
"""

import os
import sys


PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
SRC_DIR = os.path.join(PROJECT_ROOT, "src")

if SRC_DIR not in sys.path:
    sys.path.insert(0, SRC_DIR)

from hc_re_ai.http_service import main


if __name__ == "__main__":
    main()
