package ru.vspochernin.gigalearn;

import org.springframework.boot.SpringApplication;

public class TestGigalearnApplication {

	public static void main(String[] args) {
		SpringApplication.from(GigalearnApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
