package com.neatly.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class ServerApplication {

	public static void main(String[] args) {
		log.info("Starting Neatly server application");
		SpringApplication.run(ServerApplication.class, args);
	}

}
