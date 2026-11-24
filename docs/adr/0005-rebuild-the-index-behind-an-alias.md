# 0005: Rebuild the search index behind an alias, without stopping search

## Status

Accepted

## Context

The index definition (analyzers, fields) changes over time, and a changed mapping cannot be applied to an existing
index. The index can also drift or be lost. Since the index is only a copy of PostgreSQL (ADR 0002), the fix is to
build a new one, but search must keep working and changes made during the rebuild must not be lost.

## Decision

- The application reads and writes through the alias `jobs`. The real index has a timestamped name such as
  `jobs-20261124-195620-506`.
- A rebuild (`POST /api/admin/reindex`, or automatically at startup when the alias is missing or the stored hash of
  `jobs-index.json` differs):
  1. creates the new index and tells the indexer to write every change to the old **and** the new index;
  2. copies all jobs from PostgreSQL into the new index in batches of 1,000 with the bulk API, using version 1;
  3. refreshes the new index, moves the alias in one atomic step, stops the double writes and deletes the old index.
- Change events carry LSN versions (ADR 0003), which are always higher than 1. So a change that reached the new index
  during step 2 is never overwritten by the older copy from PostgreSQL, and a deleted job is not brought back.
- Elasticsearch runs with `action.auto_create_index: false`, so a write can never create an index with a guessed
  mapping.

## Consequences

- Searches never see a missing or half-filled index; they switch to the new one at once.
- During a rebuild every change is written twice, and the cluster briefly needs room for two copies.
- Only one rebuild runs at a time; a second request gets `409 Conflict`.
- A delete is remembered by Elasticsearch for `index.gc_deletes` (60 seconds). A rebuild that takes longer than that
  could, in a rare race, copy a job that was deleted while it was running. Running the rebuild again removes it.
