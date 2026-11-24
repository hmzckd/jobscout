# 0004: Copy the company name into each job document

## Status

Accepted

## Context

Users search for jobs by company name ("Kuzey Yazılım backend"). Elasticsearch has no joins at query time, so the
company name must live inside each job document. In PostgreSQL it lives once, in `companies`.

## Decision

The indexer looks up the company name when it indexes a job. When a company is renamed, Debezium publishes the change
on `jobscout.public.companies` and the indexer updates every job of that company with one `update_by_query` (after a
refresh, so jobs indexed a moment earlier are included).

## Consequences

- Searching and showing the company name costs nothing at query time.
- A rename touches every job of the company. Fine for rare renames, too slow for data that changes all the time.
- `update_by_query` bumps the internal version of each document by one. The next change of that job comes with a much
  higher LSN, so the LSN rule of ADR 0003 still holds.
- There is a small race: if a job event reads the old name just before the rename commits and is written just after
  the bulk update, that job keeps the old name until its next change. A reindex repairs it.
