package com.hmzckd.jobscout;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@TestConfiguration(proxyBeanMethods = false)
@RestController
class ThreadProbe {

	@GetMapping("/test/thread")
	String thread() {
		return "virtual=" + Thread.currentThread().isVirtual();
	}
}
