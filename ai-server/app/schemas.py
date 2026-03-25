from pydantic import BaseModel, Field
from typing import List, Optional, Any

# --- Recommendation ---
class RecommendRequest(BaseModel):
    user_profile: dict
    top_k: Optional[int] = 3

class RecommendItem(BaseModel):
    id: Any
    boardType: Optional[str] = None
    title: str
    category: Optional[str] = None
    tags: List[str] = []
    reason: Optional[str] = None

class RecommendResponse(BaseModel):
    message: str
    totalCount: int
    data: List[RecommendItem]

class SavePostEmbeddingRequest(BaseModel):
    post_id: str
    post_doc: dict

class SavePostEmbeddingResponse(BaseModel):
    success: bool
    message: Optional[str] = None

# --- Summarization ---
class SummarizeRequest(BaseModel):
    post_content: str
    language: Optional[str] = "ko"

class SummarizeResponse(BaseModel):
    summary: str
    keywords: List[str]

# --- Auto Tagging ---
class AutoTagRequest(BaseModel):
    boardType: str
    title: str
    body: str
    category: str

class AutoTagResponse(BaseModel):
    message: str
    tags: List[str]
