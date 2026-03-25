from fastapi import APIRouter, HTTPException
from app.schemas import AutoTagRequest, AutoTagResponse
from app.services.tagging import generate_auto_tags

router = APIRouter()


@router.post("/", response_model=AutoTagResponse)
def auto_tag_endpoint(request: AutoTagRequest):
    try:
        return generate_auto_tags(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
