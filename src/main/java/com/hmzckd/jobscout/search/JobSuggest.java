package com.hmzckd.jobscout.search;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;

@Service
public class JobSuggest {

	static final int MIN_PREFIX = 2;

	private final ElasticsearchClient client;
	private final JobIndex index;

	JobSuggest(ElasticsearchClient client, JobIndex index) {
		this.client = client;
		this.index = index;
	}

	public List<String> suggest(String prefix, int size) {
		String text = prefix == null ? "" : prefix.strip();
		if (text.length() < MIN_PREFIX) {
			return List.of();
		}
		try {
			SearchResponse<Void> response = client.search(s -> s
					.index(index.name())
					.size(size)
					.source(src -> src.fetch(false))
					.fields(f -> f.field("title"))
					.query(q -> q.bool(b -> b
							.must(m -> m.multiMatch(mm -> mm
									.query(text)
									.type(TextQueryType.BoolPrefix)
									.operator(Operator.And)
									.fields("title.suggest", "title.suggest._2gram", "title.suggest._3gram")))
							.should(sh -> sh.prefix(p -> p.field("title.prefix").value(text).boost(5f)))
							.filter(f -> f.term(t -> t.field("status").value("OPEN")))))
					.collapse(c -> c.field("title.keyword")),
					Void.class);
			return response.hits().hits().stream()
					.map(hit -> hit.fields().get("title"))
					.map(value -> value.to(String[].class)[0])
					.toList();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
