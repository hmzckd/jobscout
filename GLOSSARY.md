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
