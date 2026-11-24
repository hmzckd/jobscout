# Glossary

Terms used in the code, tests and docs of JobScout.

| Term | Meaning |
| --- | --- |
| Job | A job posting: title, description, city, work mode, seniority, salary range, skills. Stored in PostgreSQL. |
| Company | The employer that publishes jobs. |
| Source of truth | PostgreSQL. Every write goes there and only there. |
| Search index | The Elasticsearch copy of the jobs, shaped for searching. It can always be rebuilt from PostgreSQL. |
| CDC (change data capture) | Reading every insert, update and delete from the database's own change log instead of having the application announce them. |
| Indexer | The part of the application that reads change events from Kafka and applies them to the search index. |
| Index lag | The time between a change being committed in PostgreSQL and it becoming searchable. |
| Inverted index | The structure behind full-text search: for every word, the list of documents that contain it. Like the index at the back of a book. |
| Analyzer | The steps that turn text into searchable words (tokens): split into words, lowercase, remove stop words, stem. The same analyzer runs on documents and on queries. |
| Turkish lowercase | Lowercasing with Turkish rules: `I` becomes `ı` and `İ` becomes `i`. |
| Stemming | Cutting words down to a common root so that `mühendisleri` and `mühendis` match. The Turkish stemmer is rule based and not perfect. |
| Folding | Removing accents and Turkish letters (`ş` to `s`, `ı` to `i`) so that `gelistirici` matches `geliştirici`. Stored in the `.folded` sub-fields. |
| Mapping | The schema of the search index: which fields exist, their types and which analyzer each one uses. |
| WAL (write-ahead log) | PostgreSQL's journal of every committed change, written before the data files. Replication and CDC read it. |
| Logical decoding | Turning WAL entries into row-level changes (insert, update, delete with column values). Needs `wal_level=logical`. |
| Replication slot | A bookmark in the WAL kept by PostgreSQL for one reader (here Debezium), so no change is lost while the reader is away. |
| Kafka Connect | A Kafka runtime for connectors that move data in or out of Kafka, managed through a REST API. |
| Debezium | A set of Kafka Connect source connectors that turn database change logs into Kafka events. |
| Change event | One Debezium message: `before`, `after`, `op` (`r` snapshot read, `c` create, `u` update, `d` delete) and `source` (LSN, transaction, timestamp). |
| Snapshot | The first pass of the connector: it reads every existing row as `r` events, then continues from the WAL. |
| LSN (log sequence number) | The position of a change in the WAL. It only grows, so it orders all committed changes. |
| External version | A version number chosen by us, not by Elasticsearch. A write is accepted only if its version is higher than the stored one. |
| At-least-once delivery | Every event is delivered, but some may be delivered twice. The consumer must be idempotent. |
| Idempotent | Applying the same change twice leaves the same result as applying it once. |
| Relevance score | How well a document matches the query (BM25). Matches in rare words and in short fields score higher. |
| Boost | A weight on a field: a match in `title^3` counts three times as much as a match in `description`. |
| Fuzziness | Typo tolerance measured in edits (insert, delete, replace a letter). `AUTO` allows 0 edits for short words, 1 for 3-5 letters and 2 for longer words. |
| Filter | A yes/no condition (city, work mode, status). Filters do not change the score and Elasticsearch can cache them. |
| Highlight | The matched words wrapped in `<em>` tags, so the UI can show why a job matched. |
| Facet | A filter option with the number of matching jobs, such as `Ankara (34)`. Built with terms aggregations. |
| Post filter | A filter applied to the hits after the aggregations are computed, so a selected facet does not hide its own other options. |
| Search as you type | A field type that also indexes word prefixes and word pairs, so `java ge` can match `Java Geliştirici` while typing. |
| Normalizer | Like an analyzer for keyword fields: it lowercases and folds the whole value without splitting it into words. |
| Collapse | Returning only the best hit for each value of a field; used to show every job title once in suggestions. |
| Denormalization | Copying data (the company name) into each document so a search needs no join. The price is updating every copy when the original changes. |
