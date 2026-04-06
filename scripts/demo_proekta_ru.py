#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import textwrap
import time
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEMO_DIR = ROOT / ".demo"
APP_LOG = DEMO_DIR / "backend.log"
PID_FILE = DEMO_DIR / "backend.pid"
APP_BASE = "http://localhost:8080"
PROM_BASE = "http://localhost:9090"


def run(cmd: list[str], *, cwd: Path | None = None, input_text: str | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        cmd,
        cwd=str(cwd or ROOT),
        input=input_text,
        capture_output=True,
        text=True,
    )


def print_block(title: str, text: str = "") -> None:
    width = shutil.get_terminal_size((120, 30)).columns
    print("\n" + "=" * width)
    print(title)
    if text:
        print("-" * width)
        print(textwrap.fill(text, width=width))
    print("=" * width)


def print_table(headers: list[str], rows: list[list[str]]) -> None:
    if not rows:
        print("(нет данных)")
        return

    widths = [len(h) for h in headers]
    for row in rows:
        for i, cell in enumerate(row):
            widths[i] = min(max(widths[i], len(cell)), 32)

    def trim(value: str, width: int) -> str:
        return value if len(value) <= width else value[: width - 1] + "…"

    header_line = " | ".join(trim(h, widths[i]).ljust(widths[i]) for i, h in enumerate(headers))
    print(header_line)
    print("-" * len(header_line))
    for row in rows:
        print(" | ".join(trim(cell, widths[i]).ljust(widths[i]) for i, cell in enumerate(row)))


def http_text(url: str) -> str | None:
    try:
        with urllib.request.urlopen(url, timeout=5) as response:
            return response.read().decode("utf-8")
    except Exception:
        return None


def http_json(url: str, method: str = "GET", data: dict | None = None) -> dict | list | None:
    payload = None
    headers = {}
    if data is not None:
        payload = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=payload, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            body = response.read().decode("utf-8")
            return json.loads(body) if body else None
    except Exception:
        return None


def docker_status(container: str) -> str:
    result = run(["docker", "inspect", "-f", "{{.State.Status}}", container])
    return result.stdout.strip() if result.returncode == 0 else "missing"


def clickhouse_query(sql: str) -> list[list[str]]:
    result = run([
        "docker", "exec", "-i", "library_clickhouse", "clickhouse-client",
        "--user", "default", "--password", "clickhouse",
        "--query", textwrap.dedent(sql).strip() + "\nFORMAT TSV"
    ])
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "ошибка ClickHouse")
    rows: list[list[str]] = []
    for line in result.stdout.splitlines():
        if line.strip():
            rows.append(line.split("\t"))
    return rows


def neo4j_cypher(cypher: str) -> list[list[str]]:
    result = run([
        "docker", "exec", "-i", "library_neo4j", "cypher-shell",
        "-u", "neo4j", "-p", "password", "--format", "plain",
        textwrap.dedent(cypher).strip()
    ])
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "ошибка Neo4j")
    rows = []
    for line in result.stdout.splitlines():
        if line.strip():
            rows.append([part.strip() for part in line.split(",")])
    return rows


def postgres_query(sql: str) -> list[list[str]]:
    result = run([
        "docker", "exec", "-i", "databases_course4-postgres-1",
        "psql", "-U", "postgres", "-d", "postgres", "-At", "-c", sql
    ])
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "ошибка PostgreSQL")
    rows: list[list[str]] = []
    for line in result.stdout.splitlines():
        if line.strip():
            rows.append(line.split("|"))
    return rows


def start_services(clean_postgres: bool = False) -> None:
    print_block("Шаг 1. Поднимаю инфраструктуру", "PostgreSQL, MongoDB, Neo4j, Kafka, ClickHouse, Prometheus, Grafana и экспортеры.")
    if clean_postgres:
        print("Делаю чистый старт PostgreSQL, чтобы backend корректно применил миграции.")
        run(["docker", "rm", "-f", "databases_course4-postgres-1"])
        run(["docker", "volume", "rm", "databases_course4_postgres_data"])

    result = run([
        "docker-compose", "up", "-d",
        "postgres", "mongodb", "neo4j",
        "kafka1", "kafka2", "kafka3",
        "clickhouse",
        "postgres-exporter", "mongodb-exporter", "neo4j-exporter",
        "prometheus", "grafana"
    ])
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "не удалось поднять docker-compose")

    time.sleep(5)


def apply_clickhouse_schema() -> None:
    print_block("Шаг 2. Проверяю схему ClickHouse", "Создаю raw-таблицу, Kafka ingestion и агрегированную витрину, если они еще не созданы.")
    sql = (ROOT / "clickhouse/init/01_kafka_ingest.sql").read_text(encoding="utf-8")
    sql += "\n" + (ROOT / "clickhouse/init/02_daily_office_mart.sql").read_text(encoding="utf-8")
    result = run([
        "docker", "exec", "-i", "library_clickhouse", "clickhouse-client",
        "--user", "default", "--password", "clickhouse", "--multiquery"
    ], input_text=sql)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "не удалось применить DDL ClickHouse")


def ensure_kafka_topics() -> None:
    print_block("Шаг 3. Проверяю Kafka topics", "Нужны topics для служебного Kafka API и для бизнес-событий библиотеки.")
    for topic in ["library_events", "library.book-events"]:
        result = run([
            "docker", "exec", "kafka1", "kafka-topics",
            "--create", "--if-not-exists",
            "--topic", topic,
            "--bootstrap-server", "kafka1:9092",
            "--partitions", "1",
            "--replication-factor", "1"
        ])
        if result.returncode != 0:
            raise RuntimeError(result.stderr.strip() or f"не удалось создать topic {topic}")
        print(f"Topic готов: {topic}")


def reset_demo_topics() -> None:
    print_block("Шаг 3а. Очищаю business topics для чистого demo", "Удаляю старый поток library.book-events и внутренние Kafka Streams topics, чтобы backend не падал на старых мусорных сообщениях.")
    topics_result = run(["docker", "exec", "kafka1", "kafka-topics", "--bootstrap-server", "kafka1:9092", "--list"])
    if topics_result.returncode != 0:
        raise RuntimeError(topics_result.stderr.strip() or "не удалось получить список topics")

    topics = [line.strip() for line in topics_result.stdout.splitlines() if line.strip()]
    to_delete = [topic for topic in topics if topic == "library.book-events" or topic.startswith("book-stats-app-")]

    for topic in to_delete:
        run([
            "docker", "exec", "kafka1", "kafka-topics",
            "--bootstrap-server", "kafka1:9092",
            "--delete",
            "--topic", topic
        ])
        print(f"Удален topic: {topic}")

    time.sleep(3)


def ensure_synthetic_data() -> None:
    rows = clickhouse_query("SELECT count() FROM library_analytics.book_events WHERE source = 'synthetic-load'")
    current = int(rows[0][0]) if rows else 0
    if current >= 100000:
        print_block("Шаг 4. Synthetic load уже есть", f"В ClickHouse уже лежит {current} аналитических событий, отдельная генерация не нужна.")
        return

    print_block("Шаг 4. Генерирую аналитическую нагрузку", "Создаю 100k+ событий и загружаю их в Kafka, откуда их заберет ClickHouse.")
    gen = run(["python3", "clickhouse/scripts/generate_events.py", "--count", "120000", "--duplicate-rate", "0.01"], cwd=ROOT)
    if gen.returncode != 0:
        raise RuntimeError(gen.stderr.strip() or "ошибка генерации synthetic-load")
    print(gen.stdout.strip())

    load = run(["bash", "clickhouse/scripts/load_events.sh"], cwd=ROOT)
    if load.returncode != 0:
        raise RuntimeError(load.stderr.strip() or "ошибка загрузки synthetic-load")
    print(load.stdout.strip())
    time.sleep(5)


def app_is_up() -> bool:
    body = http_text(f"{APP_BASE}/")
    return body is not None


def start_backend() -> None:
    if app_is_up():
        print_block("Шаг 5. Backend уже запущен", "Ktor-приложение уже отвечает на localhost:8080.")
        return

    print_block("Шаг 5. Запускаю backend", "Стартую Ktor-приложение в фоне и жду, пока поднимется API.")
    DEMO_DIR.mkdir(exist_ok=True)
    APP_LOG.write_text("", encoding="utf-8")
    launcher = (
        f"cd {ROOT} && "
        f"nohup ./gradlew run > {APP_LOG} 2>&1 & "
        f"echo $!"
    )
    result = run(["bash", "-lc", launcher])
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "не удалось запустить backend")
    PID_FILE.write_text(result.stdout.strip() + "\n", encoding="utf-8")

    for _ in range(60):
        if app_is_up():
            print("Backend поднялся: http://localhost:8080")
            return
        time.sleep(2)

    tail = APP_LOG.read_text(encoding="utf-8", errors="ignore")[-4000:] if APP_LOG.exists() else ""
    raise RuntimeError("backend не поднялся вовремя\n" + tail)


def create_demo_entities() -> dict:
    print_block("Шаг 6. Создаю живые данные библиотеки", "Через API создаю офис, клиента, книгу, экземпляр, бронь и выдачу. Это и есть сквозной demo-flow проекта.")
    suffix = str(int(time.time()))

    office = http_json(f"{APP_BASE}/api/offices", method="POST", data={
        "name": f"Демо-офис {suffix}",
        "address": "ул. Библиотечная, 1",
        "workingTime": "10:00-20:00"
    })
    client = http_json(f"{APP_BASE}/api/clients", method="POST", data={
        "name": f"Читатель {suffix}",
        "email": f"reader_{suffix}@demo.local",
        "password": "demo-pass",
        "city": "Новосибирск"
    })
    book = http_json(f"{APP_BASE}/api/books", method="POST", data={
        "title": f"База данных и Kafka {suffix}",
        "genre": "education",
        "year": 2026,
        "description": "Демонстрационная книга для полного data-flow",
        "tags": ["demo", "clickhouse", "kafka"],
        "isbnNumber": f"ISBN-{suffix}"
    })

    if not all(isinstance(x, dict) for x in [office, client, book]):
        raise RuntimeError("не удалось создать офис/клиента/книгу через API")

    copy = http_json(f"{APP_BASE}/api/books/{book['id']}/copies", method="POST", data={
        "officeID": office["id"],
        "status": "AVAILABLE"
    })
    reservation = http_json(f"{APP_BASE}/api/reservations", method="POST", data={
        "bookCopyID": copy["id"],
        "clientID": client["id"],
        "durationInDays": 5
    })
    loan = http_json(f"{APP_BASE}/api/loans", method="POST", data={
        "bookCopyID": copy["id"],
        "clientID": client["id"],
        "durationInDays": 14
    })

    if not all(isinstance(x, dict) for x in [copy, reservation, loan]):
        raise RuntimeError("не удалось создать экземпляр/бронь/выдачу через API")

    print_table(
        ["сущность", "id"],
        [
            ["офис", str(office["id"])],
            ["клиент", str(client["id"])],
            ["книга", str(book["id"])],
            ["экземпляр", str(copy["id"])],
            ["бронь", str(reservation["id"])],
            ["выдача", str(loan["id"])],
        ],
    )

    return {
        "office": office,
        "client": client,
        "book": book,
        "copy": copy,
        "reservation": reservation,
        "loan": loan,
    }


def sync_demo_to_neo4j(data: dict) -> None:
    print_block("Шаг 7. Дополняю графовый сценарий Neo4j", "Создаю в графе читателя, экземпляр книги и похожего читателя, чтобы можно было показать графовые запросы.")
    client_id = data["client"]["id"]
    client_name = data["client"]["name"].replace("'", "")
    copy_id = data["copy"]["id"]
    other_id = client_id + 100000
    cypher = f"""
    MERGE (c1:Client {{id: '{client_id}'}})
    SET c1.name = '{client_name}'
    MERGE (copy:BookCopy {{id: '{copy_id}'}})
    MERGE (c1)-[:BORROWED]->(copy)
    MERGE (c2:Client {{id: '{other_id}'}})
    SET c2.name = 'Похожий читатель'
    MERGE (c2)-[:BORROWED]->(copy);
    """
    rows = neo4j_cypher(cypher)
    _ = rows


def show_recent_results(data: dict) -> None:
    print_block("Шаг 8. Показываю результаты по всем подсистемам", "После создания книги и выдачи проверяю PostgreSQL, ClickHouse, Neo4j и Prometheus.")

    pg_rows = postgres_query("select id, name, address from offices order by id desc limit 3;")
    print("\nPostgreSQL: последние офисы")
    print_table(["id", "name", "address"], pg_rows)

    ch_rows = clickhouse_query(f"""
        SELECT event_type, event_id, office_id, client_id, book_copy_id
        FROM library_analytics.book_events
        WHERE source = 'library-backend'
        ORDER BY ingested_at DESC
        LIMIT 10
    """)
    print("\nClickHouse: последние события от backend")
    print_table(["event_type", "event_id", "office_id", "client_id", "book_copy_id"], ch_rows)

    mart_rows = clickhouse_query(f"""
        SELECT day, office_id, loan_events, reservation_events, issued_events
        FROM library_analytics.daily_office_metrics
        WHERE office_id = {data['office']['id']}
        ORDER BY day DESC
        LIMIT 5
    """)
    print("\nClickHouse: витрина по созданному офису")
    print_table(["day", "office_id", "loan_events", "reservation_events", "issued_events"], mart_rows)

    stats = http_json(f"{APP_BASE}/api/neo4j/stats")
    similar = http_json(f"{APP_BASE}/api/neo4j/similar-readers/{data['client']['id']}")
    print("\nNeo4j: статистика графа")
    if isinstance(stats, dict):
        print_table(["nodes", "relationships"], [[str(stats.get("nodes")), str(stats.get("relationships"))]])
    else:
        print("(API Neo4j недоступно)")

    print("\nNeo4j: похожие читатели")
    if isinstance(similar, list) and similar:
        rows = [[str(x.get("id")), str(x.get("name")), str(x.get("commonBorrowedCopies"))] for x in similar]
        print_table(["id", "name", "commonBorrowedCopies"], rows)
    else:
        fallback_rows = neo4j_cypher(f"""
            MATCH (c1:Client {{id: '{data['client']['id']}'}})-[:BORROWED]->(copy:BookCopy)<-[:BORROWED]-(c2:Client)
            WHERE c1 <> c2
            RETURN c2.id, c2.name, count(copy)
        """)
        if len(fallback_rows) > 1:
            print_table(["id", "name", "commonBorrowedCopies"], fallback_rows[1:])
        else:
            print("(нет похожих читателей или API недоступно)")

    prom = http_json(f"{PROM_BASE}/api/v1/query?query=up")
    print("\nPrometheus: состояние targets")
    prom_rows: list[list[str]] = []
    if isinstance(prom, dict):
        for item in prom.get("data", {}).get("result", [])[:10]:
            prom_rows.append([
                item.get("metric", {}).get("job", "unknown"),
                item.get("metric", {}).get("instance", ""),
                str(item.get("value", ["", ""])[1]),
            ])
    print_table(["job", "instance", "up"], prom_rows)


def show_architecture_cheatsheet() -> None:
    print_block(
        "Архитектура и data-flow",
        "Основной сценарий: API библиотеки создает сущности и публикует события в Kafka. ClickHouse читает Kafka topic, складывает события в raw-таблицу и обновляет витрину. Neo4j используется для графовых запросов, Prometheus собирает метрики контейнеров и экспортеров."
    )
    print("1. Пользователь создает книгу, экземпляр, бронь и выдачу через REST API.")
    print("2. Backend пишет транзакционные данные в PostgreSQL и документы книги в MongoDB.")
    print("3. Backend публикует события BookIssued / ReservationCreated / BookLoaned в Kafka topic library.book-events.")
    print("4. ClickHouse через Kafka engine читает события и пишет их в raw-таблицу book_events.")
    print("5. Materialized view обновляет агрегированную витрину daily_office_metrics.")
    print("6. Neo4j хранит граф читателей и книг для запросов похожести.")
    print("7. Prometheus собирает метрики с exporter-ов и самих сервисов.")


def full_demo(clean_postgres: bool = False) -> None:
    show_architecture_cheatsheet()
    start_services(clean_postgres=clean_postgres)
    apply_clickhouse_schema()
    reset_demo_topics()
    ensure_kafka_topics()
    ensure_synthetic_data()
    start_backend()
    data = create_demo_entities()
    time.sleep(4)
    sync_demo_to_neo4j(data)
    show_recent_results(data)
    print_block("Демо завершено", "Теперь можно открыть scripts/project_console.py или dashboard.py, либо просто повторить показ из этого скрипта на защите.")


def stop_backend() -> None:
    if PID_FILE.exists():
        pid = PID_FILE.read_text(encoding="utf-8").strip()
        if pid:
            run(["bash", "-lc", f"kill {pid} >/dev/null 2>&1 || true"])
        PID_FILE.unlink(missing_ok=True)
    print("Фоновый backend остановлен, если он был запущен этим скриптом.")


def menu() -> None:
    while True:
        print("\nРусский demo-пульт проекта\n")
        print("1. Полное демо проекта")
        print("2. Полное демо с чистым PostgreSQL")
        print("3. Только поднять инфраструктуру")
        print("4. Только запустить backend")
        print("5. Только создать demo-данные через API")
        print("6. Показать архитектуру и data-flow")
        print("7. Остановить backend, запущенный этим скриптом")
        print("q. Выход")
        choice = input("\nВыбери пункт: ").strip().lower()

        try:
            if choice == "1":
                full_demo(clean_postgres=False)
            elif choice == "2":
                full_demo(clean_postgres=True)
            elif choice == "3":
                start_services(clean_postgres=False)
                apply_clickhouse_schema()
                ensure_kafka_topics()
                ensure_synthetic_data()
            elif choice == "4":
                start_backend()
            elif choice == "5":
                data = create_demo_entities()
                time.sleep(4)
                sync_demo_to_neo4j(data)
                show_recent_results(data)
            elif choice == "6":
                show_architecture_cheatsheet()
            elif choice == "7":
                stop_backend()
            elif choice == "q":
                return
            else:
                print("Неизвестный пункт.")
        except Exception as exc:
            print(f"\nОшибка: {exc}\n")


def main() -> None:
    if len(sys.argv) > 1 and sys.argv[1] == "--demo":
        clean = "--clean-postgres" in sys.argv[2:]
        full_demo(clean_postgres=clean)
        return
    menu()


if __name__ == "__main__":
    main()
