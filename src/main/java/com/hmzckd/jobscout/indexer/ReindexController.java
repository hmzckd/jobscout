package com.hmzckd.jobscout.indexer;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReindexController {

	private final Reindexer reindexer;

	ReindexController(Reindexer reindexer) {
		this.reindexer = reindexer;
	}

	@PostMapping("/api/admin/reindex")
	ReindexResult reindex() {
		return reindexer.rebuild();
	}
}
