# AI Service

FastAPI + LangGraph Text-to-SQL service for the cosmetics shop analytics chatbot.

## Run

```bash
cd ai-service
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn main:app --reload --port 8000
```

## Test

```bash
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"which membership type spends the most?","user_id":"1","role":"ADMIN"}'
```
