package com.hmzckd.jobscout.search;

import java.util.List;

public record JobDocument(
		String id,
		String companyId,
		String companyName,
		String title,
		String description,
		String city,
		String workMode,
		String seniority,
		Integer salaryMin,
		Integer salaryMax,
		List<String> skills,
		String status,
		String postedAt) {
}
