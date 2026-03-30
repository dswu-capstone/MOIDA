from typing import List, Optional

from pydantic import BaseModel, Field


class UserInput(BaseModel):
    memberId: Optional[str] = None
    nickname: Optional[str] = None
    major: Optional[str] = None
    interestCategory: List[str] = Field(default_factory=list)
    goal: Optional[str] = None
    availableDays: List[str] = Field(default_factory=list)
    availableTime: List[str] = Field(default_factory=list)
    introduce: Optional[str] = None
    level: Optional[str] = None
    locationType: Optional[str] = None
    region: Optional[str] = None


class PostProfile(BaseModel):
    field: Optional[str] = None
    days: List[str] = Field(default_factory=list)
    time_slot: List[str] = Field(default_factory=list)
    mode: Optional[str] = None
    region: Optional[str] = None
    level: Optional[str] = None
    keywords: List[str] = Field(default_factory=list)


class PostInput(BaseModel):
    id: Optional[str] = None
    boardType: Optional[str] = None
    title: str = ""
    body: str = ""
    category: Optional[str] = None
    tags: List[str] = Field(default_factory=list)
    availableDays: List[str] = Field(default_factory=list)
    availableTime: List[str] = Field(default_factory=list)
    locationType: Optional[str] = None
    region: Optional[str] = None
    level: Optional[str] = None
    keywords: List[str] = Field(default_factory=list)
    semantic_text: Optional[str] = None
    embedding: List[float] = Field(default_factory=list)
    profile: Optional[PostProfile] = None


class PostPreprocessRequest(BaseModel):
    post: PostInput


class PostPreprocessResult(BaseModel):
    id: Optional[str] = None
    boardType: Optional[str] = None
    title: str
    body: str
    category: Optional[str] = None
    tags: List[str] = Field(default_factory=list)
    profile: PostProfile
    semantic_text: str
    embedding: List[float] = Field(default_factory=list)


class PostPreprocessResponse(BaseModel):
    message: str
    data: PostPreprocessResult


class RecommendRequest(BaseModel):
    user_input: UserInput
    posts: List[PostInput] = Field(default_factory=list)
    top_k: Optional[int] = Field(default=None, ge=1, le=50)


class RecommendItem(BaseModel):
    id: Optional[str] = None
    title: str
    keywords: List[str] = Field(default_factory=list)
    reason: str


class RecommendResponse(BaseModel):
    message: str
    totalCount: int
    data: List[RecommendItem]
