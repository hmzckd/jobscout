package com.hmzckd.jobscout.company;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(UUID id, String name, Instant createdAt) {
}
