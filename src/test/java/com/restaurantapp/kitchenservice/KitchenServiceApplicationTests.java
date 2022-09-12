package com.restaurantapp.kitchenservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class KitchenServiceApplicationTests {

	@Test
	void contextLoads() throws InterruptedException {

		ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.schedule(()->{
			System.out.println(Instant.now());
		}, 4, TimeUnit.SECONDS);
		executorService.schedule(()->{
			System.out.println(Instant.now());
		}, 6, TimeUnit.SECONDS);
		executorService.awaitTermination(7, TimeUnit.SECONDS);
	}

}
