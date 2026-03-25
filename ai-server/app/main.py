from fastapi import FastAPI
from fastapi import Request
from app.routes.recommend import router as recommend_router
from app.routes.summarize import router as summarize_router
from app.routes.tag import router as tag_router

app = FastAPI()

@app.middleware("http")
async def force_utf8_charset(request: Request, call_next):
	response = await call_next(request)
	content_type = response.headers.get("content-type", "")
	if content_type.startswith("application/json") and "charset" not in content_type.lower():
		response.headers["content-type"] = "application/json; charset=utf-8"
	return response

app.include_router(recommend_router, prefix="/ai/recommend", tags=["Recommendation"])
app.include_router(summarize_router, prefix="/ai/summarize", tags=["Summarization"])
app.include_router(tag_router, prefix="/ai/tag", tags=["Tagging"])
