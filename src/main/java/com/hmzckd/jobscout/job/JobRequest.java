package com.hmzckd.jobscout.job;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hmzckd.jobscout.web.BadRequestException;

public record JobRequest(
		UUID companyId,
		String title,
		String description,
		String city,
		WorkMode workMode,
		Seniority seniority,
		Integer salaryMin,
		Integer salaryMax,
		List<String> skills) {

	static final int MAX_TITLE = 200;
	static final int MAX_DESCRIPTION = 20_000;
	static final int MAX_CITY = 100;
	static final int MAX_SKILLS = 20;
	static final int MAX_SKILL = 50;

	JobRequest normalized() {
		if (companyId == null) {
			throw new BadRequestException("companyId is required");
		}
		if (workMode == null) {
			throw new BadRequestException("workMode is required");
		}
		if (seniority == null) {
			throw new BadRequestException("seniority is required");
		}
		if (salaryMin != null && salaryMin <= 0 || salaryMax != null && salaryMax <= 0) {
			throw new BadRequestException("salaries must be positive");
		}
		if (salaryMin != null && salaryMax != null && salaryMin > salaryMax) {
			throw new BadRequestException("salaryMin must not be greater than salaryMax");
		}
		return new JobRequest(companyId,
				text("title", title, MAX_TITLE),
				text("description", description, MAX_DESCRIPTION),
				text("city", city, MAX_CITY),
				workMode, seniority, salaryMin, salaryMax,
				cleanSkills(skills));
	}

	private static String text(String field, String value, int max) {
		String stripped = value == null ? "" : value.strip();
		if (stripped.isEmpty() || stripped.length() > max) {
			throw new BadRequestException(field + " must be 1-" + max + " characters");
		}
		return stripped;
	}

	private static List<String> cleanSkills(List<String> skills) {
		if (skills == null) {
			return List.of();
		}
		Set<String> clean = new LinkedHashSet<>();
		for (String skill : skills) {
			String stripped = skill == null ? "" : skill.strip();
			if (stripped.isEmpty() || stripped.length() > MAX_SKILL) {
				throw new BadRequestException("each skill must be 1-" + MAX_SKILL + " characters");
			}
			clean.add(stripped);
		}
		if (clean.size() > MAX_SKILLS) {
			throw new BadRequestException("at most " + MAX_SKILLS + " skills are allowed");
		}
		return List.copyOf(clean);
	}
}
