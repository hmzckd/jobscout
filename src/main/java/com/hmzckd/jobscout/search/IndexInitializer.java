package com.hmzckd.jobscout.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class IndexInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(IndexInitializer.class);

	private final JobIndex index;

	IndexInitializer(JobIndex index) {
		this.index = index;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (index.createIfMissing()) {
			log.info("Created search index {}", index.name());
		}
	}
}
