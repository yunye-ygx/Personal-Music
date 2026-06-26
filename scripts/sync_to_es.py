"""
从 PostgreSQL 同步歌曲数据到 Elasticsearch
用法: python scripts/sync_to_es.py
"""
import json
import psycopg2
import urllib.request

# PostgreSQL 配置
PG_CONFIG = {
    "host": "192.168.100.128",
    "port": 5433,
    "dbname": "moodtune",
    "user": "postgres",
    "password": "123456"
}

# Elasticsearch 配置
ES_HOST = "http://192.168.100.128:9200"
ES_INDEX = "songs"


def parse_mood_tags(raw):
    if raw is None:
        return []
    if isinstance(raw, list):
        return raw
    s = str(raw).strip()
    if not s or s == "{}":
        return []
    if s.startswith("{") and s.endswith("}"):
        s = s[1:-1]
    return [tag.strip().strip('"') for tag in s.split(",") if tag.strip()]


def fetch_songs():
    conn = psycopg2.connect(**PG_CONFIG)
    cur = conn.cursor()
    cur.execute("SELECT id, title, artist, genre, file_url, liked, mood_tags FROM songs")
    rows = cur.fetchall()
    cur.close()
    conn.close()
    return rows


def bulk_index(songs):
    ndjson = ""
    for row in songs:
        doc = {
            "id": row[0],
            "title": row[1],
            "artist": row[2],
            "genre": row[3],
            "file_url": row[4],
            "liked": bool(row[5]),
            "mood_tags": parse_mood_tags(row[6])
        }
        ndjson += json.dumps({"index": {"_index": ES_INDEX, "_id": str(row[0])}}) + "\n"
        ndjson += json.dumps(doc, ensure_ascii=False) + "\n"

    url = f"{ES_HOST}/_bulk"
    req = urllib.request.Request(url, data=ndjson.encode("utf-8"),
                                headers={"Content-Type": "application/x-ndjson"})
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read().decode("utf-8"))

    if result.get("errors"):
        for item in result["items"]:
            if "error" in item.get("index", {}):
                print(f"  错误: {item['index']['error']}")
    return len(songs)


if __name__ == "__main__":
    print("正在从 PostgreSQL 读取歌曲数据...")
    songs = fetch_songs()
    print(f"读取到 {len(songs)} 首歌曲")

    if not songs:
        print("没有数据需要同步")
        exit(0)

    print("正在同步到 Elasticsearch...")
    count = bulk_index(songs)
    print(f"同步完成，共 {count} 首歌曲")
