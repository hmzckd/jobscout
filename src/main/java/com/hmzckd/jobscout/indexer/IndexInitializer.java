package com.hmzckd.jobscout.indexer;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
class IndexInitializer implements SmartLifecycle {

	private final Reindexer reindexer;
	private volatile boolean running;

	IndexInitializer(Reindexer reindexer) {
		this.reindexer = reindexer;
	}

	@Override
	public void start() {
		reindexer.ensureIndex();
		running = true;
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		return 0;
	}
}
