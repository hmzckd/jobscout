# 0003: Use the WAL position (LSN) as the search document version

## Status

Accepted

## Context

The indexer reads change events from Kafka with at-least-once delivery: after a crash or a rebalance, events can be
delivered again. A reindex (rebuilding the index from PostgreSQL) can also run while change events are still
flowing. In both cases an older version of a job could overwrite a newer one in the search index.

## Decision

Every Debezium event carries `source.lsn`, the position of the change in PostgreSQL's write-ahead log. It only grows,
and a later commit always has a higher LSN. The indexer writes every document and every delete with that LSN as an
Elasticsearch **external version**. Elasticsearch rejects a write whose version is not higher than the stored one;
the indexer treats that rejection as "already applied" and moves on.

Change events are keyed by the job id, so all changes of one job land in the same Kafka partition and are read in
order by one consumer.

## Consequences

- Replaying events is harmless (idempotent), and an older event can never overwrite a newer document.
- Every write to the index must carry an LSN, including the reindex (it uses the LSN read at its start).
- Elasticsearch forgets deleted versions after `index.gc_deletes` (60 seconds by default). An event older than a
  delete that arrives later than that could bring a deleted job back. With ordered partitions this needs a very late
  redelivery; a reindex fixes it.
- Errors that can be fixed by waiting (Elasticsearch down) are retried forever with exponential backoff, so no event
  is skipped. Events that can never be indexed (malformed JSON) are logged and skipped.
