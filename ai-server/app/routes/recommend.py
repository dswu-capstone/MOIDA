from fastapi import APIRouter, HTTPException
from app.schemas import RecommendRequest, RecommendResponse, SavePostEmbeddingRequest, SavePostEmbeddingResponse
from app.services.recommendation import recommend_studies, save_post_embedding

router = APIRouter()

@router.post("/", response_model=RecommendResponse)
def recommend_endpoint(request: dict):
    try:
        # 기본 포맷: {"user_profile": {...}, "top_k": 3}
        # Java 응답 포맷도 허용: {"message":..., "totalCount":..., "data":[...], "user_profile": {...}, "top_k": 3}
        user_profile = request.get("user_profile") if isinstance(request, dict) else None
        top_k = request.get("top_k", 3) if isinstance(request, dict) else 3

        if not user_profile and isinstance(request, dict):
            direct_member_id = request.get("memberId")
            if direct_member_id:
                user_profile = {"memberId": direct_member_id}

        if not user_profile:
            raise HTTPException(status_code=400, detail="user_profile 또는 memberId가 필요합니다.")

        recommend_req = RecommendRequest(user_profile=user_profile, top_k=top_k)

        candidate_posts = None
        if isinstance(request, dict):
            raw_posts = request.get("data") or request.get("posts")
            if isinstance(raw_posts, list):
                candidate_posts = []
                for p in raw_posts:
                    if not isinstance(p, dict):
                        continue
                    candidate_posts.append({
                        "_id": p.get("_id") or p.get("id"),
                        "id": p.get("id") or p.get("_id"),
                        "boardType": p.get("boardType"),
                        "title": p.get("title", ""),
                        "body": p.get("body", ""),
                        "category": p.get("category"),
                        "tags": p.get("tags", []) if isinstance(p.get("tags"), list) else [],
                        "embedding": p.get("embedding") if isinstance(p.get("embedding"), list) else None,
                        "region": p.get("region"),
                        "availableDays": p.get("availableDays", []) if isinstance(p.get("availableDays"), list) else [],
                        "availableTime": p.get("availableTime", []) if isinstance(p.get("availableTime"), list) else [],
                    })

        return recommend_studies(recommend_req, candidate_posts=candidate_posts)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/embedding", response_model=SavePostEmbeddingResponse)
def save_post_embedding_endpoint(request: SavePostEmbeddingRequest):
    try:
        save_post_embedding(request.post_id, request.post_doc)
        return SavePostEmbeddingResponse(success=True, message="embedding saved")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
