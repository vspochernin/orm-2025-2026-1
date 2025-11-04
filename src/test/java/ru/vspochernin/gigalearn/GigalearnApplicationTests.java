package ru.vspochernin.gigalearn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class GigalearnApplicationTests {

	@Container
	static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);

		// Настройки Hikari для тестов - предотвращаем зависание при завершении
		registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
		registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
		registry.add("spring.datasource.hikari.validation-timeout", () -> "3000");
		registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
	}

	@Test
	void contextLoads() {
	}

}
