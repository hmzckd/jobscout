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
