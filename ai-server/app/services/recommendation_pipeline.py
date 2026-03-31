import json
import re
from dataclasses import dataclass
from typing import List, Tuple

import numpy as np
import openai

from app.schemas import (
    PostInput,
    PostPreprocessResponse,
    PostPreprocessResult,
    PostProfile,
    TagGenerateResponse,
    RecommendItem,
    RecommendRequest,
    RecommendResponse,
    UserInput,
)
from app.utils import OPENAI_API_KEY, OPENAI_EMBEDDING_MODEL, OPENAI_KEYWORD_MODEL


OPENAI_REQUEST_TIMEOUT_SECONDS = 20
TAG_GENERATE_COUNT = 3


@dataclass
class ExtractedPostProfile:
    category: str | None
    keywords: List[str]
    available_days: List[str]
    available_time: List[str]
    level: str | None
    location_type: str | None
    region: str | None


def _get_openai_client():
    if not OPENAI_API_KEY:
        return None
    return openai.OpenAI(api_key=OPENAI_API_KEY)


def _norm_str(value: str | None) -> str:
    return str(value or "").strip().lower()


def _norm_list(values: List[str]) -> List[str]:
    results: List[str] = []
    for value in values:
        token = _norm_str(value)
        if token and token not in results:
            results.append(token)
    return results


def _norm_location_type(value: str | None) -> str:
    token = _norm_str(value)
    if token in {"online", "on-line", "온라인", "줌", "zoom", "디스코드", "discord", "화상", "비대면"}:
        return "online"
    if token in {"offline", "off-line", "오프라인", "대면", "직접", "현장", "오프"}:
        return "offline"
    return token


def _norm_region(value: str | None) -> str:
    token = _norm_str(value)
    return token.replace("특별시", "").replace("광역시", "").replace("시", "")


def _norm_level(value: str | None) -> str:
    token = _norm_str(value)
    if token in {"입문", "초급", "beginner", "초보", "초보자", "초급자"}:
        return "beginner"
    if token in {"중급", "intermediate", "초중급", "중급자"}:
        return "intermediate"
    if token in {"고급", "상급", "advanced", "고급자", "상급자"}:
        return "advanced"
    return token


def _normalize_days(values: List[str]) -> List[str]:
    day_map = {
        "월": "mon", "월요일": "mon", "monday": "mon", "mon": "mon",
        "화": "tue", "화요일": "tue", "tuesday": "tue", "tue": "tue",
        "수": "wed", "수요일": "wed", "wednesday": "wed", "wed": "wed",
        "목": "thu", "목요일": "thu", "thursday": "thu", "thu": "thu",
        "금": "fri", "금요일": "fri", "friday": "fri", "fri": "fri",
        "토": "sat", "토요일": "sat", "saturday": "sat", "sat": "sat",
        "일": "sun", "일요일": "sun", "sunday": "sun", "sun": "sun",
    }
    normalized: List[str] = []
    for value in values:
        token = _norm_str(value)
        mapped = day_map.get(token, token)
        if mapped and mapped not in normalized:
            normalized.append(mapped)
    return normalized


def _normalize_times(values: List[str]) -> List[str]:
    time_map = {
        "오전": "morning", "아침": "morning", "morning": "morning", "am": "morning", "9시": "morning", "10시": "morning", "11시": "morning",
        "오후": "afternoon", "낮": "afternoon", "afternoon": "afternoon", "pm": "afternoon",
        "저녁": "evening", "밤": "evening", "evening": "evening", "night": "evening", "19시": "evening", "20시": "evening", "21시": "evening", "퇴근 후": "evening",
    }
    normalized: List[str] = []
    for value in values:
        token = _norm_str(value)
        mapped = time_map.get(token, token)
        if mapped and mapped not in normalized:
            normalized.append(mapped)
    return normalized


def _list_overlap(left: List[str], right: List[str]) -> bool:
    if not left or not right:
        return True
    return bool(set(left) & set(right))


def _fallback_keywords(text: str, max_keywords: int = 8) -> List[str]:
    tokens = re.findall(r"[A-Za-z가-힣0-9+#._-]+", text.lower())
    stopwords = {
        "그리고", "또는", "에서", "으로", "하는", "있는", "합니다", "입니다", "the", "and", "for", "with",
        "this", "that", "스터디", "프로젝트", "모집",
    }
    results: List[str] = []
    for token in tokens:
        if len(token) < 2 or token in stopwords:
            continue
        if token not in results:
            results.append(token)
        if len(results) >= max_keywords:
            break
    return results


def _fallback_post_profile(post: PostInput) -> ExtractedPostProfile:
    raw_text = f"{post.title} {post.body}".lower()
    keywords = _fallback_keywords(f"{post.title} {post.body} {' '.join(post.tags)}")

    days = _normalize_days(post.availableDays)
    times = _normalize_times(post.availableTime)
    location_type = _norm_location_type(post.locationType)
    region = _norm_region(post.region)

    if not location_type:
        if any(token in raw_text for token in ["온라인", "zoom", "줌", "디스코드", "discord"]):
            location_type = "online"
        elif any(token in raw_text for token in ["오프라인", "대면"]):
            location_type = "offline"

    if not days:
        regex_days = re.findall(r"(월요일|화요일|수요일|목요일|금요일|토요일|일요일|월|화|수|목|금|토|일)", raw_text)
        days = _normalize_days(regex_days)

    if not times:
        hits: List[str] = []
        if any(token in raw_text for token in ["오전", "아침", "am", "9시", "10시", "11시"]):
            hits.append("morning")
        if any(token in raw_text for token in ["오후", "낮", "pm"]):
            hits.append("afternoon")
        if any(token in raw_text for token in ["저녁", "밤", "evening", "night", "19시", "20시", "21시", "퇴근 후"]):
            hits.append("evening")
        times = _norm_list(hits)

    return ExtractedPostProfile(
        category=_norm_str(post.category) or None,
        keywords=keywords,
        available_days=days,
        available_time=times,
        level=_norm_level(post.level) or None,
        location_type=location_type or None,
        region=region or None,
    )


def _extract_post_profile_llm(post: PostInput) -> ExtractedPostProfile:
    client = _get_openai_client()
    if client is None:
        return _fallback_post_profile(post)

    natural_text = f"제목: {post.title}\n본문: {post.body}"
    prompt = (
        "다음 모집 게시글에서 추천용 구조 필드를 추출하세요. JSON 한 줄만 출력하세요.\n"
        "형식:\n"
        '{"category":"", "keywords":[], "availableDays":[], "availableTime":[], '
        '"level":"", "locationType":"", "region":""}\n\n'
        "규칙:\n"
        "- keywords: 핵심 주제/활동 중심 최대 8개\n"
        "- availableDays: 요일만\n"
        "- availableTime: 오전/오후/저녁\n"
        "- 정보가 없으면 빈 문자열/빈 배열\n"
        f"\n게시글:\n{natural_text}"
    )

    try:
        completion = client.chat.completions.create(
            model=OPENAI_KEYWORD_MODEL,
            temperature=0.1,
            messages=[{"role": "user", "content": prompt}],
            timeout=OPENAI_REQUEST_TIMEOUT_SECONDS,
        )
        content = completion.choices[0].message.content or ""
        data = json.loads(content)

        keywords_raw = data.get("keywords", []) if isinstance(data, dict) else []
        days_raw = data.get("availableDays", []) if isinstance(data, dict) else []
        times_raw = data.get("availableTime", []) if isinstance(data, dict) else []

        return ExtractedPostProfile(
            category=_norm_str(data.get("category", "")) or _norm_str(post.category) or None,
            keywords=_norm_list([str(item) for item in keywords_raw])[:8] or _fallback_keywords(natural_text),
            available_days=_normalize_days([str(item) for item in days_raw]),
            available_time=_normalize_times([str(item) for item in times_raw]),
            level=_norm_level(data.get("level", "")) or _norm_level(post.level) or None,
            location_type=_norm_location_type(data.get("locationType", "")) or _norm_location_type(post.locationType) or None,
            region=_norm_region(data.get("region", "")) or _norm_region(post.region) or None,
        )
    except Exception:
        return _fallback_post_profile(post)


def _extract_tags_llm(post: PostInput) -> List[str]:
    client = _get_openai_client()
    if client is None:
        return []

    natural_text = f"제목: {post.title}\n본문: {post.body}\n카테고리: {post.category or ''}"
    prompt = (
        "다음 모집 게시글에서 화면에 보여줄 태그를 3개만 추출하세요. JSON 한 줄만 출력하세요.\n"
        "형식: {\"tags\":[\"...\",\"...\",\"...\"]}\n"
        "규칙:\n"
        f"- 정확히 최대 {TAG_GENERATE_COUNT}개\n"
        "- 너무 일반적인 단어(예: 모집, 스터디, 참여) 지양\n"
        "- 짧은 키워드/명사형 중심\n"
        "- 중복/유사어 중복 금지\n"
        "- 언어는 게시글 언어를 따름\n"
        f"\n게시글:\n{natural_text}"
    )

    try:
        completion = client.chat.completions.create(
            model=OPENAI_KEYWORD_MODEL,
            temperature=0.1,
            messages=[{"role": "user", "content": prompt}],
            timeout=OPENAI_REQUEST_TIMEOUT_SECONDS,
        )
        content = (completion.choices[0].message.content or "").strip()
        content = re.sub(r"^```(?:json)?\s*", "", content)
        content = re.sub(r"\s*```$", "", content)
        data = json.loads(content)
        tags_raw = data.get("tags", []) if isinstance(data, dict) else []
        tags = _norm_list([str(item) for item in tags_raw])
        return tags[:TAG_GENERATE_COUNT]
    except Exception:
        return []


# def _fallback_embedding(feature_text: str, dim: int = 256) -> np.ndarray:
def _fallback_embedding(feature_text: str, dim: int = 1536) -> np.ndarray:
    vector = np.zeros(dim, dtype=float)
    if not feature_text.strip():
        return vector
    for token in re.findall(r"[A-Za-z가-힣0-9+#._-]+", feature_text.lower()):
        index = hash(token) % dim
        vector[index] += 1.0
    norm = float(np.linalg.norm(vector))
    return vector if norm == 0.0 else vector / norm


def _embedding_feature(feature_text: str) -> np.ndarray:
    client = _get_openai_client()
    if client is None:
        return _fallback_embedding(feature_text)
    try:
        response = client.embeddings.create(
            model=OPENAI_EMBEDDING_MODEL,
            input=feature_text,
            timeout=OPENAI_REQUEST_TIMEOUT_SECONDS,
        )
        vector = np.array(response.data[0].embedding, dtype=float)
        norm = float(np.linalg.norm(vector))
        return vector if norm == 0.0 else vector / norm
    except Exception:
        return _fallback_embedding(feature_text)


def _cosine_similarity(user_vector: np.ndarray, post_vector: np.ndarray) -> float:
    denominator = float(np.linalg.norm(user_vector) * np.linalg.norm(post_vector))
    if denominator == 0.0:
        return 0.0
    return float(np.dot(user_vector, post_vector) / denominator)


def _build_user_feature_text(user: UserInput) -> str:
    fields = [
        user.major or "",
        " ".join(user.interestCategory),
        user.goal or "",
        user.introduce or "",
    ]
    return " ".join([field for field in fields if field]).lower()


def _build_post_feature_text(post: PostInput, extracted: ExtractedPostProfile) -> str:
    fields = [
        post.boardType or "",
        post.title or "",
        post.body or "",
        extracted.category or "",
        " ".join(extracted.keywords),
    ]
    return " ".join([field for field in fields if field]).lower()


def _passes_filter(user: UserInput, extracted: ExtractedPostProfile) -> bool:
    user_region = _norm_region(user.region)
    user_days = _normalize_days(user.availableDays)
    user_times = _normalize_times(user.availableTime)

    if user_region and extracted.region and user_region != extracted.region:
        return False

    if user_times and extracted.available_time and not _list_overlap(user_times, extracted.available_time):
        return False

    if user_days and extracted.available_days and not _list_overlap(user_days, extracted.available_days):
        return False

    return True


def _interest_soft_bonus(user: UserInput, extracted: ExtractedPostProfile) -> float:
    user_interest = _norm_list(user.interestCategory)
    if not user_interest:
        return 0.0

    bonus = 0.0
    user_interest_set = set(user_interest)

    if extracted.category and extracted.category in user_interest_set:
        bonus += 0.06

    if extracted.keywords:
        overlap_count = len(set(extracted.keywords) & user_interest_set)
        bonus += min(0.04, overlap_count * 0.02)

    return min(0.10, bonus)


def _extract_from_preprocessed(post: PostInput) -> ExtractedPostProfile:
    if post.profile:
        return ExtractedPostProfile(
            category=_norm_str(post.profile.field) or _norm_str(post.category) or None,
            keywords=_norm_list(post.profile.keywords),
            available_days=_normalize_days(post.profile.days),
            available_time=_normalize_times(post.profile.time_slot),
            level=_norm_level(post.profile.level) or _norm_level(post.level) or None,
            location_type=_norm_location_type(post.profile.mode) or _norm_location_type(post.locationType) or None,
            region=_norm_region(post.profile.region) or _norm_region(post.region) or None,
        )

    return ExtractedPostProfile(
        category=_norm_str(post.category) or None,
        keywords=_norm_list(post.keywords),
        available_days=_normalize_days(post.availableDays),
        available_time=_normalize_times(post.availableTime),
        level=_norm_level(post.level) or None,
        location_type=_norm_location_type(post.locationType) or None,
        region=_norm_region(post.region) or None,
    )


def _is_preprocessed_post(post: PostInput) -> bool:
    has_embedding = len(post.embedding) > 0
    if not has_embedding:
        return False

    if post.profile:
        return True

    return bool(post.keywords or post.availableDays or post.availableTime or post.region)


def _post_dedup_key(post: PostInput) -> str:
    if post.id:
        return f"id:{_norm_str(post.id)}"
    return "|".join([
        "content",
        _norm_str(post.boardType),
        _norm_str(post.title),
        _norm_str(post.body),
    ])


def _resolve_top_k(requested_top_k: int | None, candidate_count: int) -> int:
    if candidate_count <= 0:
        return 0

    if requested_top_k is not None:
        return min(requested_top_k, candidate_count)

    if candidate_count <= 3:
        return candidate_count
    if candidate_count <= 10:
        return min(5, candidate_count)
    if candidate_count <= 30:
        return min(8, candidate_count)
    return min(10, candidate_count)


def _reason_template(user: UserInput, extracted: ExtractedPostProfile, score: float) -> str:
    reasons: List[str] = []
    user_interest = set(_norm_list(user.interestCategory))
    overlap = [keyword for keyword in extracted.keywords if keyword in user_interest]

    if extracted.category and extracted.category in user_interest:
        reasons.append(f"관심 있는 {extracted.category} 주제와 잘 맞습니다")
    elif overlap:
        reasons.append(f"관심 키워드인 {', '.join(overlap[:2])}와 연결됩니다")

    if user.region and extracted.region and _norm_region(user.region) == extracted.region:
        reasons.append("선호 지역 조건에도 맞는 모집글입니다")

    if user.availableDays and extracted.available_days and _list_overlap(_normalize_days(user.availableDays), extracted.available_days):
        reasons.append("가능한 요일이 겹쳐 참여하기 좋습니다")

    if user.availableTime and extracted.available_time and _list_overlap(_normalize_times(user.availableTime), extracted.available_time):
        reasons.append("가능한 시간대와 일정이 잘 맞습니다")

    if not reasons and (user.goal or user.introduce):
        reasons.append("목표와 소개 내용을 기준으로 유사한 활동이라 추천드립니다")

    if not reasons:
        reasons.append(f"프로필과 게시글의 의미 유사도가 높아 추천드립니다(유사도 {score:.3f})")

    return "\n".join(reasons[:2])


def _reason_with_llm(
    user: UserInput,
    post: PostInput,
    extracted: ExtractedPostProfile,
    score: float,
) -> str:
    fallback_reason = _reason_template(user, extracted, score)
    client = _get_openai_client()
    if client is None:
        return fallback_reason

    user_payload = {
        "major": user.major,
        "interestCategory": user.interestCategory,
        "goal": user.goal,
        "availableDays": user.availableDays,
        "availableTime": user.availableTime,
        "introduce": user.introduce,
        "level": user.level,
        "locationType": user.locationType,
        "region": user.region,
    }
    post_payload = {
        "title": post.title,
        "boardType": post.boardType,
        "category": extracted.category or post.category,
        "keywords": extracted.keywords,
        "days": extracted.available_days,
        "time": extracted.available_time,
        "region": extracted.region,
        "mode": extracted.location_type,
        "level": extracted.level,
        "score": round(float(score), 4),
    }

    prompt = (
        "사용자에게 보여줄 추천 이유를 한국어로 1~2줄 생성하세요.\n"
        "조건:\n"
        "- 최대 2줄, 각 줄은 간결하게\n"
        "- 존댓말\n"
        "- 나열식(예: A 일치, B 일치) 대신 자연스러운 설명\n"
        "- 실제 매칭된 정보(관심분야/키워드/요일/시간/지역/수준) 중심\n"
        "- 과장/허위 표현 금지\n"
        "- 결과는 JSON 한 줄: {\"reason\":\"...\"}\n\n"
        f"user={json.dumps(user_payload, ensure_ascii=False)}\n"
        f"post={json.dumps(post_payload, ensure_ascii=False)}"
    )

    try:
        completion = client.chat.completions.create(
            model=OPENAI_KEYWORD_MODEL,
            temperature=0.2,
            messages=[{"role": "user", "content": prompt}],
            timeout=OPENAI_REQUEST_TIMEOUT_SECONDS,
        )
        content = (completion.choices[0].message.content or "").strip()
        content = re.sub(r"^```(?:json)?\s*", "", content)
        content = re.sub(r"\s*```$", "", content)
        data = json.loads(content)
        reason = str(data.get("reason", "")).strip()
        reason = "\n".join([line.strip() for line in reason.splitlines() if line.strip()][:2])
        return reason or fallback_reason
    except Exception:
        return fallback_reason


def _to_post_profile(extracted: ExtractedPostProfile) -> PostProfile:
    return PostProfile(
        field=extracted.category,
        days=extracted.available_days,
        time_slot=extracted.available_time,
        mode=extracted.location_type,
        region=extracted.region,
        level=extracted.level,
        keywords=extracted.keywords,
    )


def preprocess_post(post: PostInput) -> PostPreprocessResponse:
    extracted = _extract_post_profile_llm(post)
    semantic_text = _build_post_feature_text(post, extracted)
    embedding = _embedding_feature(semantic_text).tolist()

    result = PostPreprocessResult(
        id=post.id,
        boardType=post.boardType,
        title=post.title,
        body=post.body,
        category=post.category,
        tags=post.tags,
        profile=_to_post_profile(extracted),
        semantic_text=semantic_text,
        embedding=embedding,
    )

    return PostPreprocessResponse(message="게시글 전처리 성공", data=result)


def generate_tags(post: PostInput) -> TagGenerateResponse:
    tags = _extract_tags_llm(post)
    if not tags:
        extracted = _extract_post_profile_llm(post)
        tags = extracted.keywords[:TAG_GENERATE_COUNT]
    if not tags:
        tags = _fallback_keywords(f"{post.title} {post.body} {' '.join(post.tags)}")[:TAG_GENERATE_COUNT]
    return TagGenerateResponse(message="AI 태그 생성 성공", tags=tags)


def recommend_posts(request: RecommendRequest) -> RecommendResponse:
    if not request.posts:
        return RecommendResponse(message="추천 게시글 조회 성공", totalCount=0, data=[])

    user_feature_text = _build_user_feature_text(request.user_input)
    user_vector = _embedding_feature(user_feature_text)

    deduped_posts: List[PostInput] = []
    seen_keys: set[str] = set()
    for post in request.posts:
        key = _post_dedup_key(post)
        if key in seen_keys:
            continue
        seen_keys.add(key)
        deduped_posts.append(post)

    filtered_rows: List[Tuple[PostInput, ExtractedPostProfile]] = []
    for post in deduped_posts:
        if not _is_preprocessed_post(post):
            continue

        extracted = _extract_from_preprocessed(post)

        if _passes_filter(request.user_input, extracted):
            filtered_rows.append((post, extracted))

    if not filtered_rows:
        return RecommendResponse(message="추천 게시글 조회 성공", totalCount=0, data=[])

    scored_rows: List[Tuple[float, PostInput, ExtractedPostProfile]] = []
    for post, extracted in filtered_rows:
        raw_vector = np.array(post.embedding, dtype=float)

        # 차원이 다르면 건너뛰기
        if raw_vector.shape[0] != user_vector.shape[0]:
            continue

        norm = float(np.linalg.norm(raw_vector))
        post_vector = raw_vector if norm == 0.0 else raw_vector / norm

        semantic_score = _cosine_similarity(user_vector, post_vector)
        score = min(1.0, semantic_score + _interest_soft_bonus(request.user_input, extracted))
        scored_rows.append((score, post, extracted))

    scored_rows.sort(key=lambda row: row[0], reverse=True)
    top_k = _resolve_top_k(request.top_k, len(scored_rows))

    items: List[RecommendItem] = []
    for idx in range(top_k):
        score, post, extracted = scored_rows[idx]
        reason = _reason_with_llm(request.user_input, post, extracted, score)
#         items.append(
#             RecommendItem(
#                 title=post.title,
#                 keywords=extracted.keywords,
#                 reason=reason,
#             )
#         )
        items.append(
            RecommendItem(
                id=post.id,
                title=post.title,
                keywords=extracted.keywords,
                reason=reason,
            )
        )

    return RecommendResponse(
        message="추천 게시글 조회 성공",
        totalCount=len(items),
        data=items,
    )
