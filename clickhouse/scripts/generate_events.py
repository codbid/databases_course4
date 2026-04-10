#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import random
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import uuid4


@dataclass(frozen=True)
class EventTypeSpec:
    name: str
    weight: int


EVENT_TYPES = [
    EventTypeSpec("BookLoaned", 55),
    EventTypeSpec("ReservationCreated", 30),
    EventTypeSpec("BookIssued", 15),
]


def weighted_event_type() -> str:
    population = [spec.name for spec in EVENT_TYPES]
    weights = [spec.weight for spec in EVENT_TYPES]
    return random.choices(population, weights=weights, k=1)[0]


def bursty_timestamp(base_start: datetime) -> datetime:
    day_offset = random.randint(0, 41)
    ts = base_start + timedelta(days=day_offset)

    burst_hour = random.choices([10, 13, 18, 21], weights=[15, 40, 35, 10], k=1)[0]
    minute = random.randint(0, 59)
    second = random.randint(0, 59)

    if ts.weekday() >= 5 and random.random() < 0.35:
        burst_hour = random.choice([11, 12, 14])

    return ts.replace(hour=burst_hour, minute=minute, second=second)


def build_event(index: int, base_start: datetime) -> dict:
    event_type = weighted_event_type()
    book_copy_id = random.randint(1, 7000)
    client_id = random.randint(1, 6000)
    book_id = random.randint(1, 2500)
    office_id = random.randint(1, 12)
    timestamp = bursty_timestamp(base_start).isoformat().replace("+00:00", "Z")

    payload: dict[str, int | str] = {
        "bookCopyId": book_copy_id,
    }

    entity_prefix = "event"
    if event_type == "BookLoaned":
        payload["loanId"] = index
        payload["clientId"] = client_id
        entity_prefix = "loan"
    elif event_type == "ReservationCreated":
        payload["reservationId"] = index
        payload["clientId"] = client_id
        entity_prefix = "reservation"
    else:
        payload["bookId"] = book_id
        payload["officeId"] = office_id
        payload["status"] = random.choice(["AVAILABLE", "UNAVAILABLE", "IN_REPAIR"])
        entity_prefix = "book-copy"

    return {
        "eventId": str(uuid4()),
        "eventType": event_type,
        "entityId": f"{entity_prefix}-{index}",
        "timestamp": timestamp,
        "source": "library-backend",
        "version": 1,
        "payload": payload,
    }


def generate_events(count: int, duplicate_rate: float) -> list[dict]:
    random.seed(42)
    base_start = datetime.now(timezone.utc) - timedelta(days=42)
    events: list[dict] = []

    for index in range(1, count + 1):
        event = build_event(index, base_start)
        events.append(event)

        if random.random() < duplicate_rate:
            duplicate = json.loads(json.dumps(event))
            events.append(duplicate)

    random.shuffle(events)
    return events


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate Kafka events aligned with EventBuilder schema.")
    parser.add_argument("--count", type=int, default=120000, help="Base number of unique events.")
    parser.add_argument("--duplicate-rate", type=float, default=0.01, help="Fraction of duplicate events.")
    parser.add_argument("--output", type=Path, default=Path("clickhouse/data/book_events.ndjson"))
    args = parser.parse_args()

    events = generate_events(args.count, args.duplicate_rate)
    args.output.parent.mkdir(parents=True, exist_ok=True)

    with args.output.open("w", encoding="utf-8") as fh:
        for event in events:
            fh.write(json.dumps(event, ensure_ascii=True) + "\n")

    print(f"Generated {len(events)} rows into {args.output}")


if __name__ == "__main__":
    main()
