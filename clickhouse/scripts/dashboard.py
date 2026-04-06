#!/usr/bin/env python3
from __future__ import annotations

import shutil
import subprocess
import sys
import textwrap
from dataclasses import dataclass


CLICKHOUSE_CMD = [
    "docker",
    "exec",
    "-i",
    "library_clickhouse",
    "clickhouse-client",
    "--user",
    "default",
    "--password",
    "clickhouse",
]


@dataclass(frozen=True)
class QueryView:
    key: str
    title: str
    description: str
    sql: str


VIEWS = [
    QueryView(
        "1",
        "Overview",
        "Key totals for raw events, unique event ids, weekly coverage and mart rows.",
        """
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
        FORMAT TSVWithNames
        """,
    ),
    QueryView(
        "2",
        "Event Mix",
        "Distribution of BookLoaned, ReservationCreated and BookIssued events.",
        """
        SELECT event_type, count() AS events
        FROM library_analytics.book_events
        GROUP BY event_type
        ORDER BY events DESC
        FORMAT TSVWithNames
        """,
    ),
    QueryView(
        "3",
        "Top Offices",
        "Top offices by aggregated loan volume from the mart.",
        """
        SELECT
            office_id,
            sum(loan_events) AS loans,
            sum(reservation_events) AS reservations,
            sum(issued_events) AS issued
        FROM library_analytics.daily_office_metrics
        GROUP BY office_id
        ORDER BY loans DESC
        LIMIT 10
        FORMAT TSVWithNames
        """,
    ),
    QueryView(
        "4",
        "Peak Hours",
        "Hours with the highest event activity in the raw table.",
        """
        SELECT
            toHour(coalesce(event_time, ingested_at)) AS hour_of_day,
            count() AS events
        FROM library_analytics.book_events
        GROUP BY hour_of_day
        ORDER BY events DESC, hour_of_day
        LIMIT 12
        FORMAT TSVWithNames
        """,
    ),
    QueryView(
        "5",
        "Raw vs Mart",
        "Compare raw BookLoaned counts and mart loan totals for office 3.",
        """
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
        FORMAT TSVWithNames
        """,
    ),
    QueryView(
        "6",
        "Duplicates Check",
        "Control query for duplicate event ids in the raw table.",
        """
        SELECT
            count() AS raw_rows,
            uniqExact(event_id) AS unique_event_ids,
            count() - uniqExact(event_id) AS duplicate_rows
        FROM library_analytics.book_events
        FORMAT TSVWithNames
        """,
    ),
]


def run_query(sql: str) -> tuple[list[str], list[list[str]]]:
    command = CLICKHOUSE_CMD + ["--query", textwrap.dedent(sql).strip()]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "clickhouse query failed")

    lines = [line.rstrip("\n") for line in result.stdout.splitlines() if line.strip()]
    if not lines:
        return [], []

    headers = lines[0].split("\t")
    rows = [line.split("\t") for line in lines[1:]]
    return headers, rows


def print_table(title: str, description: str, headers: list[str], rows: list[list[str]]) -> None:
    width = shutil.get_terminal_size((120, 30)).columns
    print("=" * width)
    print(title)
    print("-" * width)
    print(textwrap.fill(description, width=width))
    print("-" * width)

    if not headers:
        print("(no rows)")
        print("=" * width)
        return

    col_widths = [len(header) for header in headers]
    for row in rows:
        for index, cell in enumerate(row):
            col_widths[index] = min(max(col_widths[index], len(cell)), 28)

    def trim(cell: str, max_width: int) -> str:
        return cell if len(cell) <= max_width else cell[: max_width - 1] + "…"

    header_line = " | ".join(trim(header, col_widths[i]).ljust(col_widths[i]) for i, header in enumerate(headers))
    print(header_line)
    print("-" * len(header_line))
    for row in rows:
        print(" | ".join(trim(cell, col_widths[i]).ljust(col_widths[i]) for i, cell in enumerate(row)))
    print("=" * width)


def print_menu() -> None:
    print("\nClickHouse Console Dashboard\n")
    for view in VIEWS:
        print(f"{view.key}. {view.title}")
    print("7. Custom SQL")
    print("q. Quit")


def execute_view(view: QueryView) -> None:
    headers, rows = run_query(view.sql)
    print_table(view.title, view.description, headers, rows)


def execute_custom_sql() -> None:
    print("\nEnter SQL terminated by an empty line:")
    lines: list[str] = []
    while True:
        line = input()
        if not line.strip():
            break
        lines.append(line)
    sql = "\n".join(lines).strip()
    if not sql:
        return
    if "format" not in sql.lower():
        sql += "\nFORMAT TSVWithNames"
    headers, rows = run_query(sql)
    print_table("Custom SQL", "Ad-hoc query result.", headers, rows)


def main() -> None:
    if len(sys.argv) > 1 and sys.argv[1] == "--summary":
        for view in VIEWS[:3]:
            execute_view(view)
        return

    while True:
        print_menu()
        choice = input("\nSelect view: ").strip().lower()
        if choice == "q":
            return
        if choice == "7":
            execute_custom_sql()
            continue

        selected = next((view for view in VIEWS if view.key == choice), None)
        if selected is None:
            print("Unknown option.\n")
            continue
        execute_view(selected)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nInterrupted.")
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)
