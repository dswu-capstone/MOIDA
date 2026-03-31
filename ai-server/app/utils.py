import os
from pathlib import Path
from dotenv import load_dotenv


BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")
load_dotenv(BASE_DIR / "app" / ".env")

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "").strip()
OPENAI_KEYWORD_MODEL = (
	os.getenv("OPENAI_KEYWORD_MODEL")
	or os.getenv("OPENAI_MODEL")
	or "gpt-4o-mini"
).strip() or "gpt-4o-mini"
OPENAI_EMBEDDING_MODEL = os.getenv("OPENAI_EMBEDDING_MODEL", "text-embedding-3-small").strip() or "text-embedding-3-small"
