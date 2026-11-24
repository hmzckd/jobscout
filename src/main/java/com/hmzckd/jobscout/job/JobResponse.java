package com.hmzckd.jobscout.job;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobResponse(
		UUID id,
		UUID companyId,
		String companyName,
		String title,
		String description,
		String city,
		WorkMode workMode,
		Seniority seniority,
		Integer salaryMin,
		Integer salaryMax,
		List<String> skills,
		JobStatus status,
		Instant postedAt,
		Instant updatedAt) {
}
