from app.schemas import RecommendRequest, RecommendResponse, RecommendItem
from app.utils import mongo_client, OPENAI_API_KEY
import openai
import numpy as np
import os
from bson import ObjectId

DAY_ALIASES = {
    "월": ["월", "월요일", "mon", "monday"],
    "화": ["화", "화요일", "tue", "tues", "tuesday"],
    "수": ["수", "수요일", "wed", "wednesday"],
    "목": ["목", "목요일", "thu", "thurs", "thursday"],
    "금": ["금", "금요일", "fri", "friday"],
    "토": ["토", "토요일", "sat", "saturday"],
    "일": ["일", "일요일", "sun", "sunday"],
}

TIME_ALIASES = {
    "오전": ["오전", "아침", "am", "morning"],
    "오후": ["오후", "낮", "pm", "afternoon"],
    "저녁": ["저녁", "밤", "evening", "night"],
}

def _get_openai_client():
    if not OPENAI_API_KEY:
        raise RuntimeError("OPENAI_API_KEY가 설정되지 않았습니다.")
    return openai.OpenAI(api_key=OPENAI_API_KEY)

def get_post_collection():
    return mongo_client["mydb"]["posts"]

def get_member_collection():
    return mongo_client["mydb"]["members"]

def member_to_text(member: dict) -> str:
    fields = [
        f"닉네임: {member.get('nickname', '')}",
        f"전공: {member.get('major', '')}",
        f"관심분야: {', '.join(member.get('interestCategory', []))}",
        f"목표: {member.get('goal', '')}",
        f"가능요일: {', '.join(member.get('availableDays', []))}",
        f"가능시간: {', '.join(member.get('availableTime', []))}",
        f"소개: {member.get('introduce', '')}",
        f"레벨: {member.get('level', '')}",
        f"모임형태: {member.get('locationType', '')}",
        f"지역: {member.get('region', '')}"
    ]
    return " | ".join([f for f in fields if f and f.split(': ')[1]])

def post_to_text(post: dict) -> str:
    fields = [
        f"제목: {post.get('title', '')}",
        f"본문: {post.get('body', '')}",
        f"카테고리: {post.get('category', '')}",
        f"태그: {', '.join(post.get('tags', []))}"
    ]
    return " | ".join([f for f in fields if f and f.split(': ')[1]])

def get_embedding(text: str, strict: bool = False) -> list:
    if not isinstance(text, str) or len(text.strip()) < 3:
        if strict:
            raise ValueError("임베딩할 텍스트가 비정상적입니다.")
        return [0.0] * 1536

    if not OPENAI_API_KEY:
        if strict:
            raise RuntimeError("OPENAI_API_KEY가 설정되지 않았습니다.")
        return [0.0] * 1536


    openai.api_type = os.getenv("OPENAI_API_TYPE", "openai")

    try:
        client = _get_openai_client()
        response = client.embeddings.create(
            input=text,
            model="text-embedding-3-small"
        )
        embedding = response.data[0].embedding
        if not isinstance(embedding, list) or len(embedding) < 10:
            raise ValueError("임베딩 결과가 비정상적입니다.")
        return embedding
    except Exception as e:
        if strict:
            raise RuntimeError(f"OpenAI 임베딩 호출 실패: {e}")
        return [0.0] * 1536

# 게시물 생성/수정 시 embedding 저장 
def save_post_embedding(post_id, post_doc):
    col = get_post_collection()
    # 1. post_id로 문서 존재 확인 
    query = {"_id": post_id}
    if isinstance(post_id, str):
        try:
            query = {"_id": ObjectId(post_id)}
        except Exception:
            query = {"_id": post_id}
    post = col.find_one(query)
    if not post:
        raise ValueError(f"해당 post_id({post_id})에 해당하는 게시물이 존재하지 않습니다.")

    # 2. post_doc 텍스트 필드 검증
    text = post_to_text(post_doc)
    if not text or len(text.strip()) < 10:
        raise ValueError("게시물 텍스트가 너무 짧거나 비어 있습니다.")

    # 3. 임베딩 생성 예외처리 
    try:
        emb = get_embedding(text, strict=True)
        if not emb or not isinstance(emb, list) or len(emb) < 10:
            raise ValueError("임베딩 결과가 비정상적입니다.")
    except Exception as e:
        import logging
        logging.error(f"임베딩 생성 실패: {e}")
        raise RuntimeError(f"임베딩 생성 실패: {e}")

    # 4. DB 업데이트
    result = col.update_one(query, {"$set": {"embedding": emb}})
    if result.matched_count == 0:
        raise RuntimeError(f"임베딩 저장 실패: post_id({post_id}) 문서 없음")

def llm_reason(user_profile, post):
    prompt = f"""
    사용자의 프로필: {user_profile}\n스터디/게시물 정보: {post}\n
    위 사용자가 해당 스터디/게시물에 적합한 이유를 한글로 1~2문장으로 설명해줘.
    """
    client = _get_openai_client()
    model_name = os.getenv("OPENAI_REASON_MODEL", "gpt-4o-mini")
    try:
        completion = client.chat.completions.create(
            model=model_name,
            messages=[{"role": "user", "content": prompt}]
        )
        return completion.choices[0].message.content.strip()
    except Exception:
        return "사용자 관심분야와 게시물 주제의 적합도가 높아 추천합니다."

def _normalize_set(values) -> set:
    if not isinstance(values, list):
        return set()
    return {str(v).strip().lower() for v in values if str(v).strip()}

def _canonical_day(day: str) -> str:
    token = str(day).strip().lower()
    for canonical, aliases in DAY_ALIASES.items():
        if token == canonical or token in aliases:
            return canonical
    return token

def _canonical_time(time_value: str) -> str:
    token = str(time_value).strip().lower()
    for canonical, aliases in TIME_ALIASES.items():
        if token == canonical or token in aliases:
            return canonical
    return token

def _match_ratio(user_values: set, post_values: set, post_text: str, alias_map: dict) -> float:
    if not user_values:
        return 0.0

    matched = 0
    for value in user_values:
        aliases = alias_map.get(value, [value])
        by_field = any(alias in post_values for alias in aliases)
        by_text = any(alias in post_text for alias in aliases)
        if by_field or by_text:
            matched += 1
    return matched / max(len(user_values), 1)

def _get_post_id(post: dict):
    return post.get("_id") or post.get("id")

def recommend_studies(request: RecommendRequest, candidate_posts: list | None = None) -> RecommendResponse:
    user_profile = request.user_profile
    top_k = request.top_k or 5
    member = get_member_collection().find_one({"memberId": user_profile.get("memberId")})
    if not member:
        member = user_profile
    user_text = member_to_text(member)
    user_emb = np.array(get_embedding(user_text))
    if candidate_posts is None:
        posts = list(get_post_collection().find({}, {"_id": 1, "id": 1, "boardType": 1, "title": 1, "body": 1, "category": 1, "tags": 1, "embedding": 1, "region": 1, "availableDays": 1, "availableTime": 1}))
    else:
        posts = candidate_posts
    scored = []
    for p in posts:
        # 임베딩이 있으면 유사도 계산, 없으면 룰 베이스
        if "embedding" in p and isinstance(p["embedding"], list) and len(p["embedding"]) > 0:
            post_emb = np.array(p["embedding"])
            sim = float(np.dot(user_emb, post_emb) / (np.linalg.norm(user_emb) * np.linalg.norm(post_emb) + 1e-8))
     
        rule_score = 0
        # 관심분야와 태그/카테고리 겹침
        user_interests = set(member.get("interestCategory", []))
        post_tags = set(p.get("tags", []))
        if user_interests & post_tags:
            rule_score += 0.1
        if p.get("category") in user_interests:
            rule_score += 0.1
        # 가능요일/시간 겹침 
        user_days = {_canonical_day(d) for d in member.get("availableDays", []) if str(d).strip()}
        user_times = {_canonical_time(t) for t in member.get("availableTime", []) if str(t).strip()}

        post_days = {_canonical_day(d) for d in p.get("availableDays", []) if str(d).strip()} if isinstance(p.get("availableDays"), list) else set()
        post_times = {_canonical_time(t) for t in p.get("availableTime", []) if str(t).strip()} if isinstance(p.get("availableTime"), list) else set()

        post_text = f"{p.get('title', '')} {p.get('body', '')} {p.get('category', '')} {' '.join(p.get('tags', []))}".lower()

        day_ratio = _match_ratio(user_days, post_days, post_text, DAY_ALIASES)
        time_ratio = _match_ratio(user_times, post_times, post_text, TIME_ALIASES)

        rule_score += 0.05 * day_ratio
        rule_score += 0.05 * time_ratio
        # 지역 일치
        if member.get("region") and p.get("region") and member.get("region") == p.get("region"):
            rule_score += 0.1
        total_score = sim + rule_score
        scored.append((total_score, p))
    scored.sort(reverse=True, key=lambda x: x[0])
    recommendations = []
    for sim, p in scored[:top_k]:
        reason = llm_reason(user_profile, p)
        post_id = _get_post_id(p)
        recommendations.append(
            RecommendItem(
                id=str(post_id) if post_id is not None else "",
                boardType=p.get("boardType"),
                title=p.get("title", ""),
                category=p.get("category"),
                tags=p.get("tags", []) if isinstance(p.get("tags"), list) else [],
                reason=reason,
            )
        )
    return RecommendResponse(
        message="추천 게시글 조회 성공",
        totalCount=len(recommendations),
        data=recommendations,
    )
