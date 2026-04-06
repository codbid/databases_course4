#!/usr/bin/env python3
from __future__ import annotations

import json
import shutil
import subprocess
import sys
import textwrap
import urllib.error
import urllib.request
from dataclasses import dataclass


APP_BASE = "http://localhost:8080"
PROM_BASE = "http://localhost:9090"


@dataclass(frozen=True)
class ServiceCheck:
    label: str
    container: str
    note: str


SERVICES = [
    ServiceCheck("PostgreSQL", "databases_course4-postgres-1", "transaction data"),
    ServiceCheck("MongoDB", "library_mongo", "document storage"),
    ServiceCheck("Neo4j", "library_neo4j", "graph relations"),
    ServiceCheck("Kafka 1", "kafka1", "event broker"),
    ServiceCheck("Kafka 2", "kafka2", "event broker"),
    ServiceCheck("Kafka 3", "kafka3", "event broker"),
    ServiceCheck("ClickHouse", "library_clickhouse", "analytics"),
]


def run(cmd: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, capture_output=True, text=True)


def docker_status(container: str) -> str:
    result = run(["docker", "inspect", "-f", "{{.State.Status}}", container])
    return result.stdout.strip() if result.returncode == 0 else "missing"


def http_json(url: str, method: str = "GET", data: dict | None = None) -> dict | list | None:
    payload = None
    headers = {}
    if data is not None:
        payload = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=payload, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=4) as response:
            body = response.read().decode("utf-8")
            if not body:
                return None
            return json.loads(body)
    except Exception:
        return None


def clickhouse_query(sql: str) -> tuple[list[str], list[list[str]]]:
    result = run([
        "docker", "exec", "-i", "library_clickhouse", "clickhouse-client",
        "--user", "default", "--password", "clickhouse",
        "--query", textwrap.dedent(sql).strip() + "\nFORMAT TSVWithNames"
    ])
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "clickhouse query failed")

    lines = [line for line in result.stdout.splitlines() if line.strip()]
    if not lines:
        return [], []
    headers = lines[0].split("\t")
    rows = [line.split("\t") for line in lines[1:]]
    return headers, rows


def neo4j_query(cypher: str) -> tuple[list[str], list[list[str]]]:
    result = run([
        "docker", "exec", "-i", "library_neo4j", "cypher-shell",
        "-u", "neo4j", "-p", "password", "--format", "plain",
        textwrap.dedent(cypher).strip()
    ])
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "neo4j query failed")

    lines = [line.rstrip() for line in result.stdout.splitlines() if line.strip()]
    if len(lines) < 2:
        return [], []
    headers = [part.strip() for part in lines[0].split(",")]
    rows = [[part.strip() for part in line.split(",")] for line in lines[1:]]
    return headers, rows


def print_table(title: str, description: str, headers: list[str], rows: list[list[str]]) -> None:
    width = shutil.get_terminal_size((120, 30)).columns
    print("=" * width)
    print(title)
    print("-" * width)
    print(textwrap.fill(description, width=width))
    print("-" * width)

    if not headers:
        print("(no rows or source unavailable)")
        print("=" * width)
        return

    col_widths = [len(header) for header in headers]
    for row in rows:
        for idx, cell in enumerate(row):
            col_widths[idx] = min(max(col_widths[idx], len(cell)), 28)

    def trim(text: str, width_: int) -> str:
        return text if len(text) <= width_ else text[: width_ - 1] + "…"

    header_line = " | ".join(trim(h, col_widths[i]).ljust(col_widths[i]) for i, h in enumerate(headers))
    print(header_line)
    print("-" * len(header_line))
    for row in rows:
        print(" | ".join(trim(cell, col_widths[i]).ljust(col_widths[i]) for i, cell in enumerate(row)))
    print("=" * width)


def show_overview() -> None:
    rows = []
    for service in SERVICES:
        rows.append([service.label, docker_status(service.container), service.note])

    app_root = "up" if http_json(f"{APP_BASE}/") is not None else "unavailable"
    prom_up = "up" if http_json(f"{PROM_BASE}/api/v1/query?query=up") is not None else "unavailable"
    rows.append(["Ktor API", app_root, "HTTP application"])
    rows.append(["Prometheus API", prom_up, "metrics and monitoring"])
    print_table(
        "Project Overview",
        "Single console entry point for the whole databases course project.",
        ["service", "status", "role"],
        rows,
    )


def show_app_kafka() -> None:
    data = http_json(f"{APP_BASE}/api/kafka/messages")
    rows: list[list[str]] = []
    if isinstance(data, list):
        for idx, item in enumerate(data[-10:], start=1):
            rows.append([str(idx), str(item)])
    print_table(
        "App Kafka Consumer",
        "Last messages seen by the Ktor in-memory Kafka consumer at /api/kafka/messages.",
        ["#", "message"],
        rows,
    )


def show_postgres_snapshot() -> None:
    result = run([
        "docker", "exec", "-i", "databases_course4-postgres-1",
        "psql", "-U", "postgres", "-d", "postgres", "-At",
        "-c",
        "select relname, n_live_tup from pg_stat_user_tables order by relname;"
    ])
    rows: list[list[str]] = []
    if result.returncode == 0:
        for line in result.stdout.splitlines():
            if not line.strip():
                continue
            parts = line.split("|")
            if len(parts) == 2:
                rows.append(parts)
    print_table(
        "PostgreSQL Snapshot",
        "Direct query to PostgreSQL system statistics for current user tables.",
        ["table", "estimated_rows"],
        rows,
    )


def show_mongo_snapshot() -> None:
    result = run([
        "docker", "exec", "-i", "library_mongo", "mongosh", "--quiet",
        "--eval",
        "const dbRef=db.getSiblingDB('library'); "
        "dbRef.getCollectionNames().forEach(c=>print(c+'\\t'+dbRef.getCollection(c).countDocuments({})));"
    ])
    rows: list[list[str]] = []
    if result.returncode == 0:
        for line in result.stdout.splitlines():
            if not line.strip():
                continue
            parts = line.split("\t")
            if len(parts) == 2:
                rows.append(parts)
    print_table(
        "MongoDB Snapshot",
        "Direct query to MongoDB collections in database 'library'.",
        ["collection", "documents"],
        rows,
    )


def send_app_test_message() -> None:
    payload = {"key": "console-demo", "value": "hello from project console"}
    result = http_json(f"{APP_BASE}/api/kafka/test", method="POST", data=payload)
    rows = [["status", json.dumps(result, ensure_ascii=False) if result is not None else "app unavailable"]]
    print_table(
        "Kafka Test Producer",
        "POST /api/kafka/test through the backend application.",
        ["field", "value"],
        rows,
    )


def show_clickhouse_summary() -> None:
    headers, rows = clickhouse_query("""
        SELECT 'raw_events' AS metric, toString(count()) AS value
        FROM library_analytics.book_events
        UNION ALL
        SELECT 'unique_event_ids', toString(uniqExact(event_id))
        FROM library_analytics.book_events
        UNION ALL
        SELECT 'weeks_covered', toString(uniqExact(event_week))
        FROM library_analytics.book_events
        UNION ALL
        SELECT 'mart_rows', toString((SELECT count() FROM library_analytics.daily_office_metrics))
        FROM system.one
    """)
    print_table(
        "ClickHouse Summary",
        "Raw storage and mart metrics for the analytics subsystem.",
        headers,
        rows,
    )


def show_clickhouse_raw_vs_mart() -> None:
    headers, rows = clickhouse_query("""
        SELECT
            raw.day,
            raw.raw_loans,
            mart.mart_loans
        FROM
        (
            SELECT
                event_date AS day,
                countIf(event_type = 'BookLoaned') AS raw_loans
            FROM library_analytics.book_events
            WHERE office_id = 3
            GROUP BY day
        ) AS raw
        FULL OUTER JOIN
        (
            SELECT
                day,
                sum(loan_events) AS mart_loans
            FROM library_analytics.daily_office_metrics
            WHERE office_id = 3
            GROUP BY day
        ) AS mart
        USING day
        ORDER BY day
        LIMIT 14
    """)
    print_table(
        "Raw vs Mart",
        "Comparison of raw BookLoaned counts and aggregated mart values for office 3.",
        headers,
        rows,
    )


def show_neo4j_direct() -> None:
    headers, rows = neo4j_query("""
        MATCH (n)
        WITH count(n) AS nodes
        MATCH ()-[r]->()
        RETURN nodes, count(r) AS relationships
    """)
    print_table(
        "Neo4j Graph Stats",
        "Direct query to Neo4j container. Useful even if the backend API is not running.",
        headers,
        rows,
    )


def show_prometheus() -> None:
    data = http_json(f"{PROM_BASE}/api/v1/query?query=up")
    rows: list[list[str]] = []
    if isinstance(data, dict):
        results = data.get("data", {}).get("result", [])
        for item in results[:20]:
            target = item.get("metric", {}).get("job", "unknown")
            instance = item.get("metric", {}).get("instance", "")
            value = item.get("value", ["", ""])[1]
            rows.append([target, instance, value])
    print_table(
        "Prometheus Targets",
        "Direct query to Prometheus API showing currently scraped targets.",
        ["job", "instance", "up"],
        rows,
    )


def full_demo() -> None:
    show_overview()
    show_postgres_snapshot()
    show_mongo_snapshot()
    try:
        send_app_test_message()
    except Exception:
        pass
    try:
        show_app_kafka()
    except Exception:
        pass
    show_clickhouse_summary()
    show_clickhouse_raw_vs_mart()
    try:
        show_neo4j_direct()
    except Exception as exc:
        print(f"Neo4j unavailable: {exc}")
    try:
        show_prometheus()
    except Exception as exc:
        print(f"Prometheus unavailable: {exc}")


def custom_clickhouse_sql() -> None:
    print("\nEnter ClickHouse SQL terminated by an empty line:")
    lines: list[str] = []
    while True:
        line = input()
        if not line.strip():
            break
        lines.append(line)
    sql = "\n".join(lines).strip()
    if not sql:
        return
    headers, rows = clickhouse_query(sql)
    print_table("Custom ClickHouse SQL", "Ad-hoc query against analytics storage.", headers, rows)


def menu() -> None:
    while True:
        print("\nWhole Project Console\n")
        print("1. Project overview")
        print("2. PostgreSQL snapshot")
        print("3. MongoDB snapshot")
        print("4. Send backend Kafka test message")
        print("5. Show backend Kafka consumer messages")
        print("6. ClickHouse summary")
        print("7. ClickHouse raw vs mart")
        print("8. Neo4j graph stats")
        print("9. Prometheus targets")
        print("10. Full demo")
        print("11. Custom ClickHouse SQL")
        print("q. Quit")
        choice = input("\nSelect option: ").strip().lower()

        try:
            if choice == "1":
                show_overview()
            elif choice == "2":
                show_postgres_snapshot()
            elif choice == "3":
                show_mongo_snapshot()
            elif choice == "4":
                send_app_test_message()
            elif choice == "5":
                show_app_kafka()
            elif choice == "6":
                show_clickhouse_summary()
            elif choice == "7":
                show_clickhouse_raw_vs_mart()
            elif choice == "8":
                show_neo4j_direct()
            elif choice == "9":
                show_prometheus()
            elif choice == "10":
                full_demo()
            elif choice == "11":
                custom_clickhouse_sql()
            elif choice == "q":
                return
            else:
                print("Unknown option.")
        except Exception as exc:
            print(f"Error: {exc}")


def main() -> None:
    if len(sys.argv) > 1 and sys.argv[1] == "--demo":
        full_demo()
        return
    menu()


if __name__ == "__main__":
    main()
