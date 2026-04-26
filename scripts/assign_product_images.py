#!/usr/bin/env python3
import csv
import os
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import quote


DB_URL = "host=aws-1-ap-northeast-1.pooler.supabase.com port=6543 dbname=postgres user=postgres.pjujvbxuxgxulyuirlvf sslmode=require"
STOP_WORDS = {
    "and", "with", "for", "the", "set", "pack", "small", "medium", "large",
    "mini", "vintage", "retro", "classic", "deluxe", "gift", "box", "of",
    "in", "on", "to", "from", "new", "assorted", "colour", "colored",
}
PHRASE_MAP = {
    "mirror ball": "mirror ball",
    "storage jar": "storage jar",
    "office mirror": "office mirror",
    "water bottle": "water bottle",
    "tea light": "candle",
    "shopping bag": "shopping bag",
}
WORD_MAP = {
    "boombox": "speaker",
    "ipod": "music player",
    "jar": "jar",
    "pen": "pen",
    "mirror": "mirror",
    "ball": "mirror ball",
    "storage": "storage organizer",
    "office": "office supplies",
    "bottle": "water bottle",
    "candle": "candle",
    "bag": "shopping bag",
    "clock": "wall clock",
    "mug": "coffee mug",
    "lamp": "table lamp",
    "basket": "storage basket",
    "frame": "photo frame",
    "notebook": "notebook",
    "chair": "chair",
    "scarf": "scarf",
}


def run_psql(sql: str, csv_output: bool = False) -> str:
    env = os.environ.copy()
    env["PGPASSWORD"] = "PelinMasha26"
    command = ["psql", DB_URL]
    if csv_output:
        command.extend(["--csv", "-c", sql])
    else:
        command.extend(["-c", sql])
    result = subprocess.run(command, check=True, capture_output=True, text=True, env=env)
    return result.stdout


def sanitize_name(name: str) -> str:
    cleaned = re.sub(r"[^a-z0-9\s-]", " ", name.lower())
    return re.sub(r"\s+", " ", cleaned).strip()


def extract_keyword(name: str) -> str:
    cleaned = sanitize_name(name)

    for phrase, keyword in PHRASE_MAP.items():
        if phrase in cleaned:
            return keyword

    words = [word for word in cleaned.split() if word not in STOP_WORDS and len(word) > 2]
    for word in words:
        if word in WORD_MAP:
            return WORD_MAP[word]

    if words:
        return words[0]

    return "product"


def build_image_url(keyword: str) -> str:
    query = quote(keyword.replace(" ", "-"))
    return f"https://loremflickr.com/400/400/{query}"


def main() -> int:
    sql = """
        select id, name
        from public.products
        order by name, id
        limit 48
    """
    rows_csv = run_psql(sql, csv_output=True)
    reader = csv.DictReader(rows_csv.splitlines())
    updates: list[str] = []

    for row in reader:
        product_id = int(row["id"])
        keyword = extract_keyword(row["name"] or "")
        image_url = build_image_url(keyword).replace("'", "''")
        updates.append(
            f"update public.products set image_url = '{image_url}' where id = {product_id};"
        )

    if not updates:
        print("No products found.")
        return 0

    temp_sql = Path("/tmp/assign_product_images.sql")
    temp_sql.write_text("\n".join(updates) + "\n", encoding="utf-8")
    try:
        env = os.environ.copy()
        env["PGPASSWORD"] = "PelinMasha26"
        subprocess.run(["psql", DB_URL, "-f", str(temp_sql)], check=True, env=env)
    finally:
        temp_sql.unlink(missing_ok=True)

    print(f"Updated image_url for {len(updates)} products.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
