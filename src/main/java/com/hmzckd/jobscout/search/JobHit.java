package com.hmzckd.jobscout.search;

import java.util.List;
import java.util.Map;

public record JobHit(
		String id,
		String companyId,
		String companyName,
		String title,
		String city,
		String workMode,
		String seniority,
		Integer salaryMin,
		Integer salaryMax,
		List<String> skills,
		String postedAt,
		Map<String, List<String>> highlights) {

	static JobHit of(JobDocument job, Map<String, List<String>> highlights) {
		return new JobHit(job.id(), job.companyId(), job.companyName(), job.title(), job.city(), job.workMode(),
				job.seniority(), job.salaryMin(), job.salaryMax(), job.skills(), job.postedAt(), highlights);
	}
}
