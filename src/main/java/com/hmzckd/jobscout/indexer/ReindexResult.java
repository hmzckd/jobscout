package com.hmzckd.jobscout.indexer;

public record ReindexResult(String index, String replaced, int documents, long tookMillis) {
}
