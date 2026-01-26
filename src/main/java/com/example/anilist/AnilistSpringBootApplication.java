package com.example.anilist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AnilistSpringBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnilistSpringBootApplication.class, args);
	}

}
