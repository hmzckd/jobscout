package com.hmzckd.jobscout.search;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch._types.query_dsl.ZeroTermsQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;

@Service
public class JobSearch {

	static final List<String> TERM_FIELDS = List.of("title^3", "title.folded^3", "skills.folded^2",
			"companyName^2", "city.folded^2", "description", "description.folded");

	private final ElasticsearchClient client;
	private final JobIndex index;

	JobSearch(ElasticsearchClient client, JobIndex index) {
		this.client = client;
		this.index = index;
	}

	public SearchResult search(String text, int page, int size) {
		return search(SearchQuery.text(text, page, size));
	}

	public SearchResult search(SearchQuery query) {
		try {
			SearchResponse<JobDocument> response = client.search(s -> s
					.index(index.name())
					.query(buildQuery(query))
					.sort(sortOptions(query))
					.from(query.page() * query.size())
					.size(query.size())
					.trackTotalHits(t -> t.enabled(true))
					.highlight(h -> h
							.preTags("<em>").postTags("</em>")
							.fields(NamedValue.of("title", HighlightField.of(f -> f.numberOfFragments(0))))
							.fields(NamedValue.of("description",
									HighlightField.of(f -> f.fragmentSize(150).numberOfFragments(1))))),
					JobDocument.class);
			List<JobHit> hits = response.hits().hits().stream().map(JobSearch::toHit).toList();
			return new SearchResult(response.hits().total().value(), query.page(), query.size(), hits);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static Query buildQuery(SearchQuery query) {
		return Query.of(q -> q.bool(b -> {
			for (String term : query.terms()) {
				b.must(m -> m.multiMatch(mm -> mm
						.query(term)
						.fields(TERM_FIELDS)
						.type(TextQueryType.BestFields)
						.fuzziness("AUTO")
						.prefixLength(1)
						.zeroTermsQuery(ZeroTermsQuery.All)));
			}
			if (query.terms().size() > 1) {
				b.should(s -> s.matchPhrase(p -> p.field("title.folded").query(query.text()).boost(2f)));
			}
			b.filter(f -> f.term(t -> t.field("status").value("OPEN")));
			for (Filter filter : filters(query)) {
				b.filter(f -> f.terms(t -> t.field(filter.field())
						.terms(v -> v.value(filter.values().stream().map(FieldValue::of).toList()))));
			}
			if (query.minSalary() != null) {
				b.filter(f -> f.range(r -> r.number(n -> n.field("salaryMax").gte(query.minSalary().doubleValue()))));
			}
			return b;
		}));
	}

	record Filter(String field, List<String> values) {
	}

	static List<Filter> filters(SearchQuery query) {
		List<Filter> filters = new ArrayList<>();
		if (!query.cities().isEmpty()) {
			filters.add(new Filter("city", query.cities()));
		}
		if (!query.workModes().isEmpty()) {
			filters.add(new Filter("workMode", query.workModes()));
		}
		if (!query.seniorities().isEmpty()) {
			filters.add(new Filter("seniority", query.seniorities()));
		}
		if (!query.skills().isEmpty()) {
			filters.add(new Filter("skills", query.skills()));
		}
		return filters;
	}

	private static List<SortOptions> sortOptions(SearchQuery query) {
		SortOptions newest = SortOptions.of(o -> o.field(f -> f.field("postedAt").order(SortOrder.Desc)));
		SortOptions id = SortOptions.of(o -> o.field(f -> f.field("id").order(SortOrder.Asc)));
		if (query.sort() == SearchQuery.Sort.NEWEST || !query.hasText()) {
			return List.of(newest, id);
		}
		return List.of(SortOptions.of(o -> o.score(sc -> sc.order(SortOrder.Desc))), newest, id);
	}

	private static JobHit toHit(Hit<JobDocument> hit) {
		Map<String, List<String>> highlights = new TreeMap<>(hit.highlight());
		return JobHit.of(hit.source(), highlights);
	}
}
