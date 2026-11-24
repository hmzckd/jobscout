package com.hmzckd.jobscout.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hmzckd.jobscout.web.BadRequestException;

@RestController
class SearchController {

	private static final int MAX_SIZE = 50;
	private static final int MAX_RESULT_WINDOW = 10_000;

	private final JobSearch search;

	SearchController(JobSearch search) {
		this.search = search;
	}

	@GetMapping("/api/search")
	SearchResult search(@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > MAX_SIZE) {
			throw new BadRequestException("page must be >= 0 and size must be 1-" + MAX_SIZE);
		}
		if ((page + 1L) * size > MAX_RESULT_WINDOW) {
			throw new BadRequestException("only the first " + MAX_RESULT_WINDOW + " results can be paged");
		}
		return search.search(q, page, size);
	}
}
