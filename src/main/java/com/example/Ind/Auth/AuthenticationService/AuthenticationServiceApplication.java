package com.example.Ind.Auth.AuthenticationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.Instant;

@Slf4j
@SpringBootApplication
@EnableCaching
public class AuthenticationServiceApplication {

	public static void main(String[] args) {
		log.info("Starting Authentication Service Application...(TimeStamp: "+ Instant.now()+")");
		SpringApplication.run(AuthenticationServiceApplication.class, args);
		log.info("Authentication Service Application started successfully. (TimeStamp: "+ Instant.now()+")");
	}
}
