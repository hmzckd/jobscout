package com.hmzckd.jobscout.search;

import java.util.Arrays;
import java.util.List;

public record SearchQuery(
		String text,
		List<String> cities,
		List<String> workModes,
		List<String> seniorities,
		List<String> skills,
		Integer minSalary,
		Sort sort,
		int page,
		int size) {

	public enum Sort {
		RELEVANCE, NEWEST
	}

	static final int MAX_TERMS = 10;

	public SearchQuery {
		text = text == null ? "" : text.strip();
		cities = cities == null ? List.of() : List.copyOf(cities);
		workModes = workModes == null ? List.of() : List.copyOf(workModes);
		seniorities = seniorities == null ? List.of() : List.copyOf(seniorities);
		skills = skills == null ? List.of() : List.copyOf(skills);
		sort = sort == null ? Sort.RELEVANCE : sort;
	}

	public static SearchQuery text(String text, int page, int size) {
		return new SearchQuery(text, null, null, null, null, null, null, page, size);
	}

	public boolean hasText() {
		return !text.isEmpty();
	}

	public List<String> terms() {
		return Arrays.stream(text.split("\\s+")).filter(t -> !t.isBlank()).limit(MAX_TERMS).toList();
	}
}
