# AI Server (MOIDA)

자연어 기반 추천 파이프라인 API 서버입니다.

## 파이프라인

사용자 프로필
→ 게시글 전처리(LLM/규칙 기반 필드·키워드 추출)
→ 필터링(요일/시간/지역)
→ 임베딩 벡터 코사인 유사도 정렬
→ 추천 이유 생성

## 1) 설치

```powershell
cd ai-server
python -m venv ..\.venv
..\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
```

## 2) 환경변수(.env)

`ai-server/.env` 또는 `ai-server/app/.env`에 설정
.

## 3) 실행

```powershell
cd ai-server
..\.venv\Scripts\Activate.ps1
python -m uvicorn app.main:app --host 127.0.0.1 --port 8002
```

## 4) 엔드포인트

- `POST /ai/recommend/preprocess`
  - 게시글 등록 시 1회 호출하여 전처리 결과(`profile`, `semantic_text`, `embedding`)를 저장할 때 사용
- `POST /ai/recommend/`
  - 전처리 완료 게시글(embedding 필수)만 추천 대상으로 사용
  - 게시글 중복 입력 시 내부적으로 중복 제거 후 추천
  - `top_k` 생략 가능(후보 수 기반 동적 반환)
  - 추천 이유는 LLM으로 생성

요청 예시:

```json
{
  "user_input": {
    "memberId": "u1",
    "interestCategory": ["개발/IT", "AI"],
    "availableDays": ["월", "수"],
    "availableTime": ["저녁"],
    "region": "서울",
    "level": "초급"
  },
  "posts": [
    {
      "id": "p1",
      "boardType": "study",
      "title": "파이썬 AI 스터디 모집",
      "body": "매주 수요일 저녁 온라인 진행",
      "category": "개발/IT",
      "tags": ["파이썬", "AI"],
      "availableDays": ["수"],
      "availableTime": ["저녁"],
      "locationType": "온라인",
      "region": "서울"
    }
  ],
  "top_k": 3
}
```

응답 예시:

```json
{
  "message": "추천 게시글 조회 성공",
  "totalCount": 1,
  "data": [
    {
      "title": "파이썬 AI 스터디 모집",
      "keywords": ["파이썬", "ai", "스터디"],
      "reason": "파이썬과 AI에 관심이 높으신 만큼, 저녁 시간 온라인 스터디가 잘 맞습니다."
    }
  ]
}
```

## 5) 백엔드 연동 방법 (Spring)

### A. 게시글 생성/수정 시 (전처리 저장)

1. 백엔드에서 게시글 저장 직후 `POST /ai/recommend/preprocess` 호출
2. 응답의 아래 필드를 `posts` 문서에 저장
  - `profile`
  - `semantic_text`
  - `embedding`

> 권장: 전처리 실패 시 게시글 저장은 유지하고, 재시도 큐/배치로 보강

### B. 추천 조회 시

1. 백엔드에서 사용자 1명(`members`)과 후보 게시글(`posts`) 조회
2. 후보 게시글은 전처리 필드(`embedding`, `profile`)가 있는 문서 우선
3. `POST /ai/recommend/` 호출
4. 응답 `data`의 `title`, `keywords`, `reason`를 프론트에 전달
