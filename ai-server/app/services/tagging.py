from app.schemas import AutoTagRequest, AutoTagResponse
from app.utils import OPENAI_API_KEY
import openai
import json
import re


def _get_openai_client():
    if not OPENAI_API_KEY:
        raise RuntimeError("OPENAI_API_KEY가 설정되지 않았습니다.")
    return openai.OpenAI(api_key=OPENAI_API_KEY)


def _fallback_tags(request: AutoTagRequest) -> list[str]:
    seed = [request.category, request.boardType]
    title_tokens = re.findall(r"[A-Za-z가-힣0-9]+", request.title)
    body_tokens = re.findall(r"[A-Za-z가-힣0-9]+", request.body)
    merged = seed + title_tokens[:5] + body_tokens[:8]
    tags = []
    for token in merged:
        token = str(token).strip()
        if len(token) < 2:
            continue
        if token not in tags:
            tags.append(token)
        if len(tags) >= 5:
            break
    return tags if tags else [request.category, request.boardType]


def _parse_tags(output: str) -> list[str]:
    output = output.strip()

    try:
        data = json.loads(output)
        if isinstance(data, dict) and isinstance(data.get("tags"), list):
            return [str(t).strip() for t in data["tags"] if str(t).strip()]
        if isinstance(data, list):
            return [str(t).strip() for t in data if str(t).strip()]
    except Exception:
        pass

    match = re.search(r"\[(.*?)\]", output, re.DOTALL)
    if match:
        items = [x.strip().strip('"\'') for x in match.group(1).split(",")]
        return [x for x in items if x]

    lines = [line.strip("-• ").strip() for line in output.splitlines() if line.strip()]
    return lines[:5]


def generate_auto_tags(request: AutoTagRequest) -> AutoTagResponse:
    if not request.title.strip() or not request.body.strip():
        return AutoTagResponse(message="게시글 자동 태그 생성 성공", tags=_fallback_tags(request)[:5])

    prompt = f"""
다음 게시글의 태그 5개를 생성하세요.
응답은 반드시 JSON 한 줄로만 반환:
{{"tags": ["태그1", "태그2", "태그3", "태그4", "태그5"]}}

boardType: {request.boardType}
title: {request.title}
body: {request.body}
category: {request.category}
"""

    try:
        client = _get_openai_client()
        completion = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.2,
        )
        content = completion.choices[0].message.content or ""
        tags = _parse_tags(content)
        if not tags:
            tags = _fallback_tags(request)
        return AutoTagResponse(message="게시글 자동 태그 생성 성공", tags=tags[:5])
    except Exception:
        return AutoTagResponse(message="게시글 자동 태그 생성 성공", tags=_fallback_tags(request)[:5])
