# TelcoStream-BSS

TelcoStream-BSS is a telecom BSS demo project that shows how a CDR charging pipeline works end to end:

- CDRs are generated or ingested
- Kafka carries the events
- Spring Boot validates, rates, and batches them
- Redis tracks prepaid balances and idempotency
- PostgreSQL stores invoices
- a browser dashboard shows live operational output

This is a portfolio-style project, but it is built around the same architecture patterns used in real telecom backends.

## What is real and what is simulated

Real-world-ready parts:

- Kafka-based event flow
- Redis-backed balance/idempotency checks
- PostgreSQL invoice storage
- batched writes for scale
- DLQ handling for bad or fraudulent messages
- live dashboard for operator visibility
- graceful shutdown and service separation

Simulated parts:

- generated CDRs
- fake subscriber numbers
- demo tariff values
- demo prepaid balances

So the project is not live operator billing data, but the pipeline structure is real-world-like and can be turned into a real-data system later.

## Why this project is useful

This project is good for showing that you understand:

1. event ingestion at telecom scale
2. charging and rating logic
3. idempotency and duplicate protection
4. prepaid balance management
5. batch persistence into PostgreSQL
6. dashboard/ops visibility
7. DLQ handling for invalid records

If you do not have access to a telecom operator, this is still a strong showcase because the system design is correct and explainable.

## What the amounts mean

The amounts shown in the dashboard are charges calculated from the generated CDR usage:

- `VOICE` is charged by duration
- `SMS` is charged per message
- `DATA` is charged by megabytes

The displayed currency is `Tk`, which means Bangladeshi Taka. In this project it is just the demo billing currency used in the tariff rules.

These amounts are calculated values, not real customer billing data.

## High-level architecture

```text
[CDR Generator / Ingestor]
          |
          v
[Kafka: raw-network-cdrs] -----> [DLQ: raw-network-cdrs-dlq]
          |
          v
[Spring Boot BSS Middleware]
   |            |            |
   |            |            +--> [PostgreSQL invoices]
   |            +----------------> [Redis balances + idempotency]
   +-----------------------------> [Dashboard APIs]
                                      |
                                      v
                               [Operator Console]
```

## Current stack

| Layer | Technology |
| --- | --- |
| Middleware | Spring Boot |
| Streaming | Kafka |
| Cache | Redis |
| Database | PostgreSQL |
| Generator | Python |
| UI | Static HTML/CSS/JS |
| Orchestration | Docker Compose |

## How the dashboard works

The dashboard at `http://localhost:8082` polls the middleware APIs:

- `/api/stats`
- `/api/invoices`
- `/api/balance/{msisdn}`

It shows:

- total invoices
- total revenue
- invoices per minute
- revenue per minute
- live invoice feed
- subscriber balance lookup
- throughput sparkline

The dashboard does not show raw Kafka messages. It shows processed billing output from the middleware.

## How to showcase this project well

If you want to present this without real operator access, say:

> “This is a telecom charging-engine prototype that simulates CDR ingestion, rating, idempotent balance handling, batch invoice persistence, and operator visibility.”

That is the honest and strong way to explain it.

For a demo, show these three things:

1. start the generator for a short period
2. show invoices increasing on the dashboard
3. open the API and prove the numbers match

This is enough to demonstrate a real telecom backend workflow.

## What you can make real without operator access

Even without a telecom network operator, you can still make these parts more realistic:

- use realistic tariff tables
- use more realistic CDR schemas
- add more service types and charging rules
- seed anonymized subscriber data
- add real validation rules
- add better audit logs
- add authentication for the dashboard
- add metrics and alerts
- use test files that look like production CDR exports

These changes make the project closer to real-world engineering without needing actual operator systems.

## What a real-data version would need

To move from demo mode to real data mode, you would need:

- real CDR input from files, Kafka, or an integration endpoint
- real tariff rules or a tariff configuration service
- real subscriber/account mapping
- proper access control
- stronger observability and alerting
- production-grade data retention and reconciliation

## Repository layout

```text
telcostream-bss/
├── docker-compose.yml
├── sql/
│   └── init.sql
├── cdr-generator/
│   ├── generator.py
│   ├── requirements.txt
│   └── Dockerfile
├── dashboard/
│   └── index.html
└── bss-middleware/
    ├── pom.xml
    ├── Dockerfile
    └── src/
        ├── main/java/com/telcostream/bss/
        │   ├── config/
        │   ├── consumer/
        │   ├── controller/
        │   ├── entity/
        │   ├── model/
        │   ├── repository/
        │   └── service/
        └── main/resources/application.yml
```

## How to run it

Use separate terminals.

Terminal 1: start the stack

```bat
cd /d "f:\TelcoStream BSS\TelcoStream-BSS"
docker compose up -d zookeeper kafka redis postgres adminer dashboard bss-middleware
```

Terminal 2: run the generator for a short test

```bat
cd /d "f:\TelcoStream BSS\TelcoStream-BSS\cdr-generator"
.\.venv\Scripts\activate
python generator.py --rate 80 --fraud-rate 0.02 --duration 30
```

If you want even shorter testing, use `--duration 10` or `--duration 15`.

Terminal 3: check logs or query output

```bat
cd /d "f:\TelcoStream BSS\TelcoStream-BSS"
docker compose logs --tail=100 bss-middleware
```

API checks:

```bat
curl http://localhost:8080/api/invoices?limit=10
curl http://localhost:8080/api/balance/8801700000001
```

Dashboard:

- `http://localhost:8082`

## Design decisions worth explaining

- Idempotency is handled using Redis so duplicate messages do not double-charge.
- Invoices are batched before database writes to reduce pressure on PostgreSQL.
- DLQ is used for invalid or fraudulent records instead of silently dropping them.
- The dashboard reads from the API, so it reflects processed billing output, not raw messages.
- The generator is synthetic by design, so the system can be tested safely without operator data.