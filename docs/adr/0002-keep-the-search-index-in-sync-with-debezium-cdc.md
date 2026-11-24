# 0002: Keep the search index in sync with Debezium CDC through Kafka

## Status

Accepted

## Context

Jobs are written to PostgreSQL and searched in Elasticsearch, so every insert, update and delete has to reach the
search index. The options were:

1. **Dual write**: the API writes to PostgreSQL and then to Elasticsearch. If the second write fails or the process
   crashes in between, the index silently drifts, and every new write path must remember to do both.
2. **Transactional outbox**: the API writes an event row in the same transaction and a relay publishes it. Reliable,
   but every write path must still remember to write the event.
3. **Change data capture (CDC)**: read the changes from PostgreSQL's write-ahead log, which already contains every
   committed change, no matter which code or which SQL statement made it.

## Decision

Use Debezium's PostgreSQL connector, running in Kafka Connect, to publish every committed change of the `jobs` and
`companies` tables to Kafka (`jobscout.public.jobs`, `jobscout.public.companies`). The application registers the
connector at startup through the Kafka Connect REST API. The write API knows nothing about Elasticsearch.

The Kafka Connect image is built from the official `apache/kafka` image plus the Debezium plugin downloaded from
Maven Central and verified with a SHA-256 checksum.

## Consequences

- A write that commits is never lost for search; a write that rolls back never reaches search.
- Search is eventually consistent: a change becomes searchable a moment after the commit (the index lag).
- PostgreSQL must run with `wal_level=logical`. Debezium keeps a replication slot; if the connector is stopped for a
  long time, the slot holds WAL on disk until it catches up.
- More moving parts in development (Kafka and Kafka Connect), all started by `docker compose`.
- The connector uses the database owner account here; a production setup would use a dedicated role with only the
  replication and select rights it needs.
