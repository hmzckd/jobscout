package com.hmzckd.jobscout.search;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hmzckd.jobscout.web.BadRequestException;

@RestController
class SearchController {

	private static final int MAX_SIZE = 50;
	private static final int MAX_RESULT_WINDOW = 10_000;
	private static final Set<String> WORK_MODES = Set.of("ONSITE", "HYBRID", "REMOTE");
	private static final Set<String> SENIORITIES = Set.of("JUNIOR", "MID", "SENIOR", "LEAD");

	private final JobSearch search;

	SearchController(JobSearch search) {
		this.search = search;
	}

	@GetMapping("/api/search")
	SearchResult search(@RequestParam(required = false) String q,
			@RequestParam(required = false) List<String> city,
			@RequestParam(required = false) List<String> workMode,
			@RequestParam(required = false) List<String> seniority,
			@RequestParam(required = false) List<String> skill,
			@RequestParam(required = false) Integer minSalary,
			@RequestParam(defaultValue = "relevance") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > MAX_SIZE) {
			throw new BadRequestException("page must be >= 0 and size must be 1-" + MAX_SIZE);
		}
		if ((page + 1L) * size > MAX_RESULT_WINDOW) {
			throw new BadRequestException("only the first " + MAX_RESULT_WINDOW + " results can be paged");
		}
		if (minSalary != null && minSalary <= 0) {
			throw new BadRequestException("minSalary must be positive");
		}
		return search.search(new SearchQuery(q, city, allowed("workMode", workMode, WORK_MODES),
				allowed("seniority", seniority, SENIORITIES), skill, minSalary, sortOf(sort), page, size));
	}

	private static List<String> allowed(String name, List<String> values, Set<String> allowed) {
		if (values == null) {
			return List.of();
		}
		List<String> upper = values.stream().map(v -> v.strip().toUpperCase(Locale.ROOT)).toList();
		for (String value : upper) {
			if (!allowed.contains(value)) {
				throw new BadRequestException(name + " must be one of " + allowed);
			}
		}
		return upper;
	}

	private static SearchQuery.Sort sortOf(String sort) {
		return switch (sort.strip().toLowerCase(Locale.ROOT)) {
			case "relevance" -> SearchQuery.Sort.RELEVANCE;
			case "newest" -> SearchQuery.Sort.NEWEST;
			default -> throw new BadRequestException("sort must be relevance or newest");
		};
	}
}
