"""
TelcoStream-BSS CDR Generator
Simulates a telecom network emitting Call Detail Records (CDRs) onto a Kafka
topic in real time. A configurable fraction of records is intentionally
malformed / fraudulent so the downstream middleware's validation and
dead-letter-queue logic has something real to do.

Usage:
    python generator.py --rate 80 --fraud-rate 0.02 --duration 60

    --rate         records per second (default 50)
    --fraud-rate   fraction of records that are corrupt/fraud, 0.0-1.0 (default 0.02)
    --duration     seconds to run, 0 = run forever (default 60)
"""

import argparse
import json
import os
import random
import time
import uuid
from datetime import datetime, timezone

from kafka import KafkaProducer
import redis

TOPIC = "raw-network-cdrs"

SERVICE_TYPES = ["VOICE", "SMS", "DATA"]

SUBSCRIBERS = [f"8801{700000000 + i}" for i in range(200)]
CELL_IDS = [f"CELL-{i:03d}" for i in range(50)]

STARTING_BALANCE = 500.00  # Taka, seeded per subscriber in Redis


def build_producer(bootstrap_servers: str) -> KafkaProducer:
    return KafkaProducer(
        bootstrap_servers=bootstrap_servers,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        linger_ms=20,
        retries=5,
    )


def seed_balances(redis_client: redis.Redis) -> None:
    """Seed every simulated subscriber with a starting prepaid balance,
    without overwriting balances already set by a previous run."""
    pipe = redis_client.pipeline()
    for msisdn in SUBSCRIBERS:
        key = f"balance:{msisdn}"
        pipe.setnx(key, STARTING_BALANCE)
    pipe.execute()


def make_good_record() -> dict:
    service_type = random.choice(SERVICE_TYPES)
    record = {
        "recordId": str(uuid.uuid4()),
        "callingNumber": random.choice(SUBSCRIBERS),
        "calledNumber": random.choice(SUBSCRIBERS),
        "durationSeconds": None,
        "dataVolumeMb": None,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "serviceType": service_type,
        "locationCellId": random.choice(CELL_IDS),
    }
    if service_type == "VOICE":
        record["durationSeconds"] = random.randint(5, 600)
    elif service_type == "SMS":
        record["durationSeconds"] = 0
    else:  # DATA
        record["dataVolumeMb"] = round(random.uniform(1, 500), 2)
    return record


def make_bad_record() -> dict:
    """Produce one of several classes of malformed/fraudulent CDR."""
    variant = random.choice(["negative_duration", "missing_field", "bad_timestamp", "unknown_service"])
    record = make_good_record()

    if variant == "negative_duration":
        record["durationSeconds"] = -random.randint(1, 100)
    elif variant == "missing_field":
        record.pop("callingNumber", None)
    elif variant == "bad_timestamp":
        record["timestamp"] = "not-a-real-timestamp"
    elif variant == "unknown_service":
        record["serviceType"] = "UNKNOWN"

    return record


def run(bootstrap_servers: str, redis_host: str, redis_port: int, rate: float, fraud_rate: float, duration: int):
    producer = build_producer(bootstrap_servers)
    redis_client = redis.Redis(host=redis_host, port=redis_port, decode_responses=True)

    seed_balances(redis_client)

    print(f"[generator] streaming to topic '{TOPIC}' at ~{rate} rec/s, fraud_rate={fraud_rate}")
    interval = 1.0 / rate if rate > 0 else 0.1
    start = time.time()
    sent = 0

    try:
        while True:
            if duration and (time.time() - start) >= duration:
                break

            record = make_bad_record() if random.random() < fraud_rate else make_good_record()
            producer.send(TOPIC, value=record)
            sent += 1

            if sent % 500 == 0:
                producer.flush()
                print(f"[generator] sent {sent} records...")

            time.sleep(interval)
    except KeyboardInterrupt:
        print("\n[generator] stopped by user")
    finally:
        producer.flush()
        producer.close()
        print(f"[generator] total sent: {sent}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="TelcoStream-BSS CDR generator")
    parser.add_argument("--bootstrap-servers", default=os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"))
    parser.add_argument("--redis-host", default=os.environ.get("REDIS_HOST", "localhost"))
    parser.add_argument("--redis-port", type=int, default=int(os.environ.get("REDIS_PORT", 6379)))
    parser.add_argument("--rate", type=float, default=float(os.environ.get("CDR_RATE", 50)))
    parser.add_argument("--fraud-rate", type=float, default=float(os.environ.get("FRAUD_RATE", 0.02)))
    parser.add_argument("--duration", type=int, default=int(os.environ.get("CDR_DURATION", 60)), help="0 = run forever")
    args = parser.parse_args()

    run(
        bootstrap_servers=args.bootstrap_servers,
        redis_host=args.redis_host,
        redis_port=args.redis_port,
        rate=args.rate,
        fraud_rate=args.fraud_rate,
        duration=args.duration,
    )
