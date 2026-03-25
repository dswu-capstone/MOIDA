# AI Server (MOIDA)

FastAPI 기반 AI 서버입니다. 추천, 요약, 태그 생성 API를 제공합니다.

## 1) 빠른 시작 

프로젝트 루트(`MOIDA`)에서 아래 순서대로 실행하세요.

```powershell
cd ai-server
python -m venv ..\.venv
..\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
```

> 권장: `uvicorn ...` 단독 실행 대신 항상 `python -m uvicorn ...`를 사용하세요.
> (전역 Python/전역 uvicorn이 잡히면 `ModuleNotFoundError`가 발생할 수 있습니다.)

## 2) 설치 (이미 venv가 있다면)

```bash
pip install -r requirements.txt
```

## 3) 실행

```bash
python -m uvicorn app.main:app --host 127.0.0.1 --port 8002
```

### 복붙 실행 블록 

아래 3줄을 그대로 실행하면, 같은 오류를 대부분 피할 수 있습니다.

```powershell
cd C:\Users\<본인계정>\MOIDA\ai-server
..\.venv\Scripts\Activate.ps1
python -m uvicorn app.main:app --host 127.0.0.1 --port 8002
```

실행 확인:

```powershell
Invoke-WebRequest http://127.0.0.1:8002/docs -UseBasicParsing | Select-Object -ExpandProperty StatusCode
```

정상 결과: `200`

## 5) 주요 엔드포인트

- `POST /ai/recommend/`
- `POST /ai/recommend/embedding`
- `POST /ai/summarize/`
- `POST /ai/tag/`

## 6) 자주 나는 오류

- `ModuleNotFoundError: No module named 'pymongo'`
	- 원인: 전역 Python으로 실행됨
	- 해결: `..\.venv\Scripts\Activate.ps1` 후 `python -m uvicorn ...` 사용
	- 추가 확인: `python -c "import sys; print(sys.executable)"` 결과가 `.venv` 경로인지 확인

- `Error loading ASGI app. Could not import module "main"`
	- 원인: 실행 위치가 `ai-server`가 아님
	- 해결: `cd ai-server` 후 `python -m uvicorn app.main:app ...`

- 포트 충돌(`Address already in use`)
	- 해결: 포트를 바꾸거나 기존 프로세스 종료 후 재실행
	- 포트 종료 예시:

```powershell
$conn = Get-NetTCPConnection -LocalPort 8002 -ErrorAction SilentlyContinue | Select-Object -First 1
if ($conn) { Stop-Process -Id $conn.OwningProcess -Force }
```