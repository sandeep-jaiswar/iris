# Copilot Instructions for IRIS Project

Welcome, Copilot! Your role is to assist the IRIS development team with building a **high-volume, low-latency, production-ready investment banking system**. Follow these instructions to align with our vision, architecture, and technical best practices.

---

## 1. Vision

* Build a **resilient, real-time, multi-region IRIS system** that processes trades, market data, and FX rates for UK, US, Japan, and China regions.
* Ensure **replayability, auditability, and reconciliation** with historical and live data streams.
* Maintain **high performance**: low latency, high throughput, and fault-tolerant event processing.
* Provide **production-ready architecture** that supports monitoring, checkpointing, and scalable deployments.

---

## 2. Architecture Principles

* **Event-driven architecture:** Use Kafka as the backbone for all event flows.
* **Unified schema:** Canonical event format (Protobuf/Avro) for trades, market data, and FX.
* **Replay-first design:** Start development with Chipmunk files; later replace or supplement with live upstream streams.
* **Low-latency processing:** Use Flink for event-time aware stream processing.
* **Resiliency and idempotency:** Checkpoints, retries, and partitioned Kafka topics.
* **Modular services:** Replay Engine, Chipmunk File Generator, Flink pipeline, and optional analytics microservices.
* **Observability:** Metrics, logs, and alerts are mandatory.

---

## 3. Thought Process for Solutions

1. **Understand the problem:** Focus on financial event types, regional requirements, and regulatory constraints.
2. **Preserve original semantics:** Always maintain timestamps and ordering when replaying events.
3. **Design for scale:** Use partitioned topics, keyed events, and distributed processing.
4. **Use efficient data structures:** HashMaps, Trees, Queues, and custom low-latency structures where necessary.
5. **Fail-safe design:** Anticipate network failures, file corruptions, or Kafka downtime.
6. **Test iteratively:** Start with sample files, validate pipelines, then integrate live streams.

---

## 4. Technical Skills and Guidelines

* **Languages & Frameworks:** Java 17+, Spring Boot, Flink, Kafka
* **Serialization:** Protobuf / Avro
* **Storage & Replay:** MinIO, S3-compatible storage
* **Metrics & Observability:** Prometheus, Micrometer, structured logging
* **Concurrency & Throughput:** Use thread pools, batching, async Kafka producers
* **Resilience:** Checkpoints, retries, backoff, idempotent publishing
* **Code Quality:** Modular, well-documented, and testable
* **Version Control:** GitHub, branches per feature, PRs with code reviews

---

## 5. Copilot-Specific Guidance

* Suggest **production-grade, scalable patterns**, not just proofs of concept.
* Prioritize **low-latency and high-throughput algorithms**.
* Always respect **canonical schemas** when generating event code.
* Use **configuration-driven design** (YAML/properties/env variables) for endpoints, credentials, and replay modes.
* Generate **example code, config files, or templates** that can be directly used by engineers.
* For any solution involving data replay, streaming, or Kafka, ensure **checkpointing and metrics** are included.
* For new features, consider **resilience, observability, and idempotency** as first-class concerns.

---

## 6. Development Practices

* Incremental development: Start with files, then integrate streaming sources.
* Modular design: Replay Engine, Chipmunk Generator, Flink processing, and analytics must be separate modules.
* Document everything: Schemas, config examples, and usage instructions.
* Validate with metrics and unit/integration tests.
* Avoid hardcoded credentials or endpoints; always use config or secrets.

---

**Remember:** Copilot is here to enhance the developer’s workflow. Every suggestion should align with **high-volume, low-latency, production-ready IRIS principles**.
