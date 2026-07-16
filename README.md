# TelcoStream-BSS

A real-time CDR (Call Detail Record) processing and charging engine that simulates the core billing pipeline of a telecom BSS system — the kind of infrastructure operators like Grameenphone, Robi, and Banglalink run internally, and that vendors like Huawei, Amdocs, and Netcracker build for them.

The system generates simulated call/SMS/data events, streams them through Kafka, validates and rates them in a Spring Boot service, checks and deducts prepaid balances in Redis, and writes the resulting invoices to PostgreSQL in batches. A small operator dashboard shows the pipeline running live.

```
[CDR Generator]  --(JSON)-->  [Kafka: raw-network-cdrs]  --> [BSS Middleware]
                                                                  |
                                              +-------------------+-------------------+
                                              |                                       |
                                        [Redis balance]                    [Kafka: raw-network-cdrs-dlq]
                                              |                              (rejected/fraud CDRs)
                                              v
                                     [PostgreSQL invoices] <-- batched writes
                                              ^
                                              |
                                   [REST API: /api/invoices, /api/balance, /api/stats]
```

## Why I built this

Telecom billing systems come down to three hard problems: ingesting a high-volume, messy event stream reliably; rating those events against a tariff in real time without double-charging anyone; and writing the results to a database at a rate it can actually sustain. I wanted a project that forced me to deal with all three properly instead of a toy CRUD app, so this pipeline handles idempotency, batched writes, a dead-letter queue for bad data, and graceful shutdown — the same concerns a real BSS team deals with day to day.

The data is entirely simulated. I'm not connected to a real telecom operator, so a Python script plays the role of the network, generating call/SMS/data events (with a small percentage of deliberately malformed ones so the validation logic has something real to catch).

## Tech stack

| Layer | Choice | Why |
|---|---|---|
| Middleware | Java 17 + Spring Boot 3 | Standard stack for BSS vendors (Huawei/Amdocs/Netcracker are Java-heavy) |
| Streaming | Apache Kafka (KRaft mode) | Industry standard for CDR ingestion |
| Cache | Redis | Sub-millisecond prepaid balance checks |
| Database | PostgreSQL | Durable invoice storage with batched writes |
| CDR simulator | Python | Fast to iterate on, easy to inject fraud/malformed records |
| Dashboard | Static HTML/CSS/JS, no build step | Live view of the pipeline for demos |
| Orchestration | Docker Compose | Everything spins up with one command |

## Project structure

```
telcostream-bss/
├── docker-compose.yml              # Kafka, Redis, Postgres, Adminer, middleware, dashboard, generator
├── sql/
│   └── init.sql                    # DB schema
├── cdr-generator/                  # Simulated telecom network
│   ├── generator.py
│   ├── requirements.txt
│   └── Dockerfile
├── dashboard/                      # Live operator console
│   └── index.html
└── bss-middleware/                 # The Spring Boot service
    ├── pom.xml
    ├── Dockerfile
    └── src/
        ├── main/java/com/telcostream/bss/
        │   ├── BssMiddlewareApplication.java
        │   ├── config/
        │   │   ├── KafkaConsumerConfig.java
        │   │   ├── KafkaProducerConfig.java
        │   │   ├── KafkaTopicConfig.java
        │   │   └── RedisConfig.java
        │   ├── model/
        │   │   ├── CdrRecord.java
        │   │   └── ServiceType.java
        │   ├── entity/
        │   │   └── Invoice.java
        │   ├── repository/
        │   │   └── InvoiceRepository.java
        │   ├── consumer/
        │   │   └── CdrConsumer.java
        │   ├── service/
        │   │   ├── CdrValidationService.java
        │   │   ├── RatingService.java
        │   │   ├── BalanceService.java
        │   │   └── InvoiceBatchService.java
        │   └── controller/
        │       ├── InvoiceController.java
        │       ├── BalanceController.java
        │       └── StatsController.java
        ├── main/resources/application.yml
        └── test/java/.../RatingServiceTest.java
```

## Running it

**Prerequisites:** Java 17, Maven 3.9+, Docker Desktop, Python 3.10+.

1. **Start the infrastructure:**
   ```bash
   docker compose up -d zookeeper kafka redis postgres adminer
   ```
   Adminer (DB browser) is at `http://localhost:8081` — server `postgres`, user `bss`, password `bss_pw`, database `telcostream`.

2. **Build and run the middleware:**
   ```bash
   cd bss-middleware
   mvn clean package -DskipTests
   docker compose up -d --build bss-middleware
   ```

3. **Run the CDR generator** to start producing traffic:
   ```bash
   cd cdr-generator
   python3 -m venv .venv && source .venv/bin/activate
   pip install -r requirements.txt
   python generator.py --rate 80 --fraud-rate 0.02
   ```

4. **Open the dashboard:**
   ```bash
   docker compose up -d dashboard
   ```
   `http://localhost:8082` — live invoice feed, revenue meters, subscriber balance lookup.

5. **Check it directly:**
   - `curl http://localhost:8080/api/invoices` — recent invoices
   - `curl http://localhost:8080/api/balance/8801700000001` — a simulated subscriber's balance
   - The DLQ topic (`raw-network-cdrs-dlq`) holds anything the validation layer rejected

## How the pipeline actually works

Every CDR goes through `CdrConsumer`: it's validated (reject negative durations, malformed timestamps, unknown service types), checked against a Redis idempotency set so a Kafka redelivery can never double-charge someone, rated against the tariff, and handed to `InvoiceBatchService`, which buffers invoices in memory and writes them to Postgres in batches of 500 or every 2 seconds — whichever comes first — instead of one row per event. Anything that fails validation goes to a dead-letter topic rather than being silently dropped, since losing billing-relevant data isn't an acceptable failure mode even in a demo project.

## What I'd add next

The core pipeline is complete and demonstrates the main BSS engineering concerns end to end, but there's a clear list of what a production version would need on top of this:

- **Postpaid billing** — bundles, rollover, and a `TariffType` to switch between prepaid and postpaid rating logic
- **Monitoring** — Prometheus + Grafana wired into the existing `/actuator/prometheus` endpoint
- **Bulk-load writes** — replacing the batched JPA `saveAll()` with a JDBC `COPY`-based loader to compare throughput at higher volume
- **Locked-down dashboard access** — the dashboard currently allows CORS from any origin and has no auth, which is fine for a local demo but not for anything exposed beyond my own machine
- **Atomic balance deduction** — the current Redis balance update is a read-then-write, which has a small race-condition window under concurrent charges to the same subscriber; a Lua script would make it atomic
