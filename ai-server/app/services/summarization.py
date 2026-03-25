from app.schemas import SummarizeRequest, SummarizeResponse
from app.utils import mongo_client, OPENAI_API_KEY
import openai

def _get_openai_client():
    if not OPENAI_API_KEY:
        raise RuntimeError("OPENAI_API_KEY가 설정되지 않았습니다.")
    return openai.OpenAI(api_key=OPENAI_API_KEY)

def get_post_collection():
    return mongo_client["mydb"]["posts"]

def post_to_text(post: dict) -> str:
    fields = [
        post.get("title", ""),
        post.get("body", ""),
        post.get("category", ""),
        ", ".join(post.get("tags", []))
    ]
    return " | ".join([f for f in fields if f])

def summarize_post(request: SummarizeRequest) -> SummarizeResponse:
    content = request.post_content
    if not content:
        return SummarizeResponse(summary="", keywords=[])

    prompt = f"""
아래는 스터디/프로젝트 모집 게시물입니다. 반드시 아래 형식으로만 출력하세요.

요약:
1. (핵심 요약 1)
2. (핵심 요약 2)
키워드: [키워드1, 키워드2, 키워드3, 키워드4, 키워드5]

게시물 내용:
{content}
"""
    client = _get_openai_client()
    completion = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": prompt}]
    )
    output = completion.choices[0].message.content.strip()
    import re
    summary_lines = re.findall(r"\d+\.\s*(.+)", output)
    summary = " ".join([line.strip() for line in summary_lines]) if summary_lines else output.split("\n")[0].strip()
    k = re.search(r"키워드[:：]?\s*\[([^\]]+)\]", output)
    if k:
        keywords = [kw.strip() for kw in re.split(r",|;", k.group(1)) if kw.strip()]
    else:
        keywords = []
    return SummarizeResponse(summary=summary, keywords=keywords)
