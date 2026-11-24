package com.hmzckd.jobscout.search;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

@Service
public class JobSearch {

	private static final List<String> TEXT_FIELDS = List.of("title", "title.folded", "description",
			"description.folded", "skills.folded", "companyName", "city.folded");

	private final ElasticsearchClient client;
	private final JobIndex index;

	JobSearch(ElasticsearchClient client, JobIndex index) {
		this.client = client;
		this.index = index;
	}

	public SearchResult search(String text, int page, int size) {
		boolean hasText = text != null && !text.isBlank();
		Query query = hasText
				? Query.of(q -> q.multiMatch(m -> m.query(text).fields(TEXT_FIELDS)))
				: Query.of(q -> q.matchAll(m -> m));
		try {
			SearchResponse<JobDocument> response = client.search(s -> {
				s.index(index.name()).query(query).from(page * size).size(size).trackTotalHits(t -> t.enabled(true));
				if (!hasText) {
					s.sort(o -> o.field(f -> f.field("postedAt").order(SortOrder.Desc)));
				}
				return s;
			}, JobDocument.class);
			List<JobDocument> hits = response.hits().hits().stream().map(Hit::source).toList();
			return new SearchResult(response.hits().total().value(), page, size, hits);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
