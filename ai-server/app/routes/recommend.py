from fastapi import APIRouter, HTTPException

from app.schemas import (
    PostPreprocessRequest,
    PostPreprocessResponse,
    TagGenerateRequest,
    TagGenerateResponse,
    RecommendRequest,
    RecommendResponse,
)
from app.services.recommendation_pipeline import generate_tags, preprocess_post, recommend_posts

router = APIRouter()


@router.post("/preprocess", response_model=PostPreprocessResponse)
def preprocess_endpoint(request: PostPreprocessRequest):
    try:
        return preprocess_post(request.post)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error))


@router.post("/tags", response_model=TagGenerateResponse)
def generate_tags_endpoint(request: TagGenerateRequest):
    try:
        return generate_tags(request.post)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error))


@router.post("/", response_model=RecommendResponse)
def recommend_endpoint(request: RecommendRequest):
    try:
        return recommend_posts(request)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error))
