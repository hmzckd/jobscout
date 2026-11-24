package com.hmzckd.jobscout.search;

import java.util.List;
import java.util.Map;

public record SearchResult(long total, int page, int size, List<JobHit> hits, Map<String, List<FacetValue>> facets) {
}
