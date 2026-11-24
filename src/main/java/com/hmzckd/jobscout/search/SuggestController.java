package com.hmzckd.jobscout.search;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hmzckd.jobscout.web.BadRequestException;

@RestController
class SuggestController {

	private static final int MAX_SIZE = 10;

	private final JobSuggest suggest;

	SuggestController(JobSuggest suggest) {
		this.suggest = suggest;
	}

	@GetMapping("/api/suggest")
	List<String> suggest(@RequestParam(required = false) String q, @RequestParam(defaultValue = "8") int size) {
		if (size < 1 || size > MAX_SIZE) {
			throw new BadRequestException("size must be 1-" + MAX_SIZE);
		}
		return suggest.suggest(q, size);
	}
}
