import logging
import os
import re
import unicodedata
from typing import Any, Literal, Optional, TypedDict

import google.generativeai as genai
import psycopg2
import psycopg2.extras
from dotenv import load_dotenv
from fastapi import FastAPI
from langgraph.graph import END, StateGraph
from pydantic import BaseModel, Field

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ai-service")

app = FastAPI(title="Cosmetics Shop AI Service", version="1.0.0")

FORBIDDEN_SQL = re.compile(
    r"\b(insert|update|delete|drop|alter|truncate|create|grant|revoke)\b",
    re.IGNORECASE,
)
SELECT_START = re.compile(r"^\s*select\b", re.IGNORECASE)
AI_SAFE_REFERENCE = re.compile(r"\bai_safe\.", re.IGNORECASE)
LIMIT_CLAUSE = re.compile(r"\blimit\s+\d+\b", re.IGNORECASE)
REQUESTED_LIMIT_PATTERNS = [
    re.compile(r"\btop\s+(\d{1,3})\b", re.IGNORECASE),
    re.compile(r"\bfirst\s+(\d{1,3})\b", re.IGNORECASE),
    re.compile(r"\bbest\s+(\d{1,3})\b", re.IGNORECASE),
    re.compile(r"\bshow\s+(?:me\s+)?(\d{1,3})\b", re.IGNORECASE),
    re.compile(r"\blist\s+(?:the\s+)?(\d{1,3})\b", re.IGNORECASE),
    re.compile(r"\b(\d{1,3})\s+(?:urun|urunu|urunler|urunleri|products?|items?|orders?|customers?|stores?|reviews?)\b", re.IGNORECASE),
]
NUMBER_WORDS = {
    "bir": 1,
    "one": 1,
    "iki": 2,
    "two": 2,
    "uc": 3,
    "three": 3,
    "dort": 4,
    "four": 4,
    "bes": 5,
    "five": 5,
    "alti": 6,
    "six": 6,
    "yedi": 7,
    "seven": 7,
    "sekiz": 8,
    "eight": 8,
    "dokuz": 9,
    "nine": 9,
    "on": 10,
    "ten": 10,
}
NON_AI_SAFE_REFERENCE = re.compile(
    r"\b(from|join)\s+(?!ai_safe\.)([a-z_][a-z0-9_]*\.?[a-z_][a-z0-9_]*)",
    re.IGNORECASE,
)

SYSTEM_PROMPT = """
You are a strict PostgreSQL SQL generator for an e-commerce analytics database.
Return only one SQL SELECT query.
Allowed schema: ai_safe.
Allowed views:
- ai_safe.products
- ai_safe.orders
- ai_safe.order_items
- ai_safe.customer_profiles
- ai_safe.customer_segments
- ai_safe.city_customer_summary
- ai_safe.country_revenue_summary
- ai_safe.membership_summary
- ai_safe.segment_summary
- ai_safe.reviews
- ai_safe.stores
Rules:
- Output SQL only, no markdown.
- Query must start with SELECT.
- Never use INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE, CREATE, GRANT, REVOKE.
- Never reference schemas outside ai_safe.
- Never use SELECT *.
- Never expose passwords, tokens, secrets, internal costs, or private user data.
- Use LIMIT 100 or less when returning lists.
"""


class ChatRequest(BaseModel):
    question: str = Field(min_length=1)
    user_id: Optional[str] = None
    role: Literal["USER", "ADMIN"] = "USER"


class ChatResponse(BaseModel):
    generated_sql: Optional[str] = None
    rows: list[dict[str, Any]] = Field(default_factory=list)
    message: str
    error: Optional[str] = None


class ChatState(TypedDict, total=False):
    question: str
    user_id: Optional[str]
    role: str
    sql: Optional[str]
    is_safe: bool
    guardrail_passed: bool
    rows: list[dict[str, Any]]
    error: Optional[str]
    message: str
    retry_count: int
    requested_limit: Optional[int]


def analyze_question(state: ChatState) -> ChatState:
    question = state["question"].strip()
    requested_limit = extract_requested_limit(question)
    logger.info("node=analyze_question question=%s requested_limit=%s", question, requested_limit)
    return {
        **state,
        "question": question,
        "retry_count": state.get("retry_count", 0),
        "requested_limit": requested_limit,
    }


def generate_sql(state: ChatState) -> ChatState:
    retry_count = state.get("retry_count", 0)
    logger.info("node=generate_sql retry_count=%s question=%s", retry_count, state["question"])

    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not api_key:
        return {**state, "error": "GEMINI_API_KEY is not configured."}

    strict_suffix = ""
    if retry_count > 0:
        strict_suffix = (
            "\nSTRICT RETRY: The previous SQL failed safety validation. "
            "Return one safer SELECT query using only ai_safe views."
        )

    role_scope = "The user is an admin and may ask admin-safe aggregate analytics."
    if state.get("role") != "ADMIN":
        role_scope = (
            "The user is a normal user. Do not generate queries for another user's private data. "
            "Prefer aggregate ai_safe analytics."
        )

    limit_instruction = ""
    if state.get("requested_limit") is not None:
        limit_instruction = (
            f"\nThe user requested exactly {state['requested_limit']} result(s). "
            f"The SQL must use LIMIT {state['requested_limit']}."
        )

    prompt = f"{role_scope}\nQuestion: {state['question']}{limit_instruction}{strict_suffix}"
    try:
        genai.configure(api_key=api_key)
        model = genai.GenerativeModel(os.getenv("GEMINI_MODEL", "gemini-2.5-flash"))
        response = model.generate_content([SYSTEM_PROMPT, prompt])
        sql = clean_sql(response.text if response else "")
        sql = enforce_requested_limit(sql, state.get("requested_limit"))
        logger.info("node=generate_sql generated_sql=%s", sql)
        return {**state, "sql": sql, "error": None}
    except Exception:
        logger.exception("node=generate_sql error")
        return {**state, "error": "SQL generation failed."}


def safety_check(state: ChatState) -> ChatState:
    sql = state.get("sql") or ""
    reason = validate_sql(sql)
    is_safe = reason is None
    logger.info("node=safety_check is_safe=%s reason=%s sql=%s", is_safe, reason, sql if is_safe else None)
    retry_count = state.get("retry_count", 0)
    if not is_safe:
        retry_count += 1
    return {**state, "is_safe": is_safe, "error": reason, "retry_count": retry_count}


def guardrails_check(state: ChatState) -> ChatState:
    question = state["question"].lower()
    blocked_terms = [
        "ignore previous instructions",
        "system prompt",
        "password",
        "token",
        "secret",
        "all users",
        "other users",
    ]
    passed = not any(term in question for term in blocked_terms)
    error = None if passed else "Request blocked by guardrails."
    logger.info("node=guardrails_check guardrail_passed=%s error=%s", passed, error)
    return {**state, "guardrail_passed": passed, "error": error}


def execute_query(state: ChatState) -> ChatState:
    sql = state.get("sql")
    logger.info("node=execute_query sql=%s", sql)
    try:
        rows = run_select(sql)
        logger.info("node=execute_query row_count=%s", len(rows))
        return {**state, "rows": rows, "error": None}
    except Exception:
        logger.exception("node=execute_query error")
        return {**state, "rows": [], "error": "Database query failed."}


def format_response(state: ChatState) -> ChatState:
    rows = state.get("rows") or []
    message = "Query executed successfully."
    if not rows:
        message = "No data found for this question."
    logger.info("node=format_response message=%s", message)
    return {**state, "message": message, "error": None}


def error_handler(state: ChatState) -> ChatState:
    logger.warning("node=error_handler error=%s", state.get("error"))
    return {
        **state,
        "sql": None,
        "rows": [],
        "message": "I could not complete that analytics request safely.",
        "error": state.get("error") or "Request failed.",
    }


def after_safety(state: ChatState) -> str:
    if state.get("is_safe"):
        return "guardrails_check"
    if state.get("retry_count", 0) == 1:
        return "generate_sql"
    return "error_handler"


def after_guardrails(state: ChatState) -> str:
    return "execute_query" if state.get("guardrail_passed") else "error_handler"


def after_execute(state: ChatState) -> str:
    return "error_handler" if state.get("error") else "format_response"


def build_graph():
    workflow = StateGraph(ChatState)
    workflow.add_node("analyze_question", analyze_question)
    workflow.add_node("generate_sql", generate_sql)
    workflow.add_node("safety_check", safety_check)
    workflow.add_node("guardrails_check", guardrails_check)
    workflow.add_node("execute_query", execute_query)
    workflow.add_node("format_response", format_response)
    workflow.add_node("error_handler", error_handler)

    workflow.set_entry_point("analyze_question")
    workflow.add_edge("analyze_question", "generate_sql")
    workflow.add_edge("generate_sql", "safety_check")
    workflow.add_conditional_edges("safety_check", after_safety)
    workflow.add_conditional_edges("guardrails_check", after_guardrails)
    workflow.add_conditional_edges("execute_query", after_execute)
    workflow.add_edge("format_response", END)
    workflow.add_edge("error_handler", END)
    return workflow.compile()


chat_graph = build_graph()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    initial_state: ChatState = {
        "question": request.question,
        "user_id": request.user_id,
        "role": request.role,
        "sql": None,
        "is_safe": False,
        "guardrail_passed": False,
        "rows": [],
        "error": None,
        "message": "",
        "retry_count": 0,
        "requested_limit": None,
    }
    result = chat_graph.invoke(initial_state)
    return ChatResponse(
        generated_sql=result.get("sql"),
        rows=result.get("rows") or [],
        message=result.get("message") or "Query executed successfully.",
        error=result.get("error"),
    )


def clean_sql(raw_sql: str) -> str:
    return (
        (raw_sql or "")
        .replace("```sql", "")
        .replace("```SQL", "")
        .replace("```", "")
        .strip()
        .rstrip(";")
    )


def extract_requested_limit(question: str) -> Optional[int]:
    normalized = normalize_question_text(question)
    for pattern in REQUESTED_LIMIT_PATTERNS:
        match = pattern.search(normalized)
        if match:
            return clamp_limit(int(match.group(1)))

    for word, value in NUMBER_WORDS.items():
        if re.search(rf"\b(top|first|best|show|list|en iyi|en yuksek)\s+(?:me\s+)?{word}\b", normalized):
            return value
        if re.search(rf"\b{word}\s+(?:urun|urunu|urunler|urunleri|products?|items?|orders?|customers?|stores?|reviews?)\b", normalized):
            return value

    return None


def normalize_question_text(question: str) -> str:
    normalized = (question or "").strip().lower()
    translation = str.maketrans({"ı": "i", "ğ": "g", "ü": "u", "ş": "s", "ö": "o", "ç": "c"})
    normalized = normalized.translate(translation)
    normalized = unicodedata.normalize("NFD", normalized)
    normalized = "".join(character for character in normalized if unicodedata.category(character) != "Mn")
    return re.sub(r"\s+", " ", re.sub(r"[^a-z0-9\s]", " ", normalized)).strip()


def clamp_limit(value: int) -> int:
    return max(1, min(value, 100))


def enforce_requested_limit(sql: str, requested_limit: Optional[int]) -> str:
    if requested_limit is None or not sql.strip():
        return sql

    safe_limit = clamp_limit(requested_limit)
    normalized = sql.strip().rstrip(";")
    if LIMIT_CLAUSE.search(normalized):
        return LIMIT_CLAUSE.sub(f"LIMIT {safe_limit}", normalized, count=1)
    return f"{normalized}\nLIMIT {safe_limit}"


def validate_sql(sql: str) -> Optional[str]:
    normalized = sql.strip()
    lowered = normalized.lower()
    if not normalized:
        return "Generated SQL is empty."
    if not SELECT_START.search(normalized):
        return "Only SELECT queries are allowed."
    if FORBIDDEN_SQL.search(normalized):
        return "SQL contains a forbidden write or DDL operation."
    if "select *" in lowered:
        return "SELECT * is not allowed."
    if ";" in normalized:
        return "Only one SQL statement is allowed."
    if not AI_SAFE_REFERENCE.search(normalized):
        return "SQL must reference the ai_safe schema."
    if NON_AI_SAFE_REFERENCE.search(normalized):
        return "SQL may only reference ai_safe schema objects."
    return None


def run_select(sql: Optional[str]) -> list[dict[str, Any]]:
    if not sql:
        raise ValueError("SQL is required.")

    database_url = os.getenv("SUPABASE_DB_URL") or os.getenv("DATABASE_URL")
    if not database_url:
        raise RuntimeError("SUPABASE_DB_URL or DATABASE_URL is not configured.")

    timeout_ms = int(os.getenv("AI_SQL_QUERY_TIMEOUT_MS", "8000"))
    max_rows = int(os.getenv("AI_SQL_MAX_ROWS", "100"))

    with psycopg2.connect(database_url) as connection:
        connection.set_session(readonly=True, autocommit=True)
        with connection.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cursor:
            cursor.execute("set statement_timeout = %s", (timeout_ms,))
            cursor.execute(sql)
            rows = cursor.fetchmany(max_rows)
            return [dict(row) for row in rows]
