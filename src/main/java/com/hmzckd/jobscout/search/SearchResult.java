package com.hmzckd.jobscout.search;

import java.util.List;

public record SearchResult(long total, int page, int size, List<JobHit> hits) {
}
