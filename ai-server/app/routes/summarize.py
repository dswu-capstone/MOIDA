from fastapi import APIRouter, HTTPException
from app.schemas import SummarizeRequest, SummarizeResponse
from app.services.summarization import summarize_post

router = APIRouter()

@router.post("/", response_model=SummarizeResponse)
def summarize_endpoint(request: SummarizeRequest):
    try:
        return summarize_post(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
