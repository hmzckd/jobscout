# 0001: Use the official Elasticsearch Java client, not Spring Data Elasticsearch

## Status

Accepted

## Context

Search quality is the point of this project: field weights, typo tolerance, filters, facet counts and suggestions.
Spring Data Elasticsearch builds queries from repository method names and annotations, which hides the query that
is actually sent.

## Decision

Talk to Elasticsearch through the official `elasticsearch-java` client that Spring Boot auto-configures. The index
settings and mappings live in one JSON file (`src/main/resources/elasticsearch/jobs-index.json`) and every query is
built explicitly in code.

## Consequences

- Every query can be read, explained and tuned in one place.
- More code than a repository interface: mapping documents to and from JSON and paging are ours to write.
- The index definition is versioned with the code; changing it means creating a new index (see later ADRs).
