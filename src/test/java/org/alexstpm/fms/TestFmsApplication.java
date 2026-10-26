package org.alexstpm.fms;

import org.springframework.boot.SpringApplication;

public class TestFmsApplication {

	public static void main(String[] args) {
		SpringApplication.from(FmsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
