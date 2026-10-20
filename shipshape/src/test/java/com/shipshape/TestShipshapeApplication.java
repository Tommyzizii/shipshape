package com.shipshape;

import org.springframework.boot.SpringApplication;

public class TestShipshapeApplication {

	public static void main(String[] args) {
		SpringApplication.from(ShipshapeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
