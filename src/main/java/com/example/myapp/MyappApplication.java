package com.example.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyappApplication {

	public static void main(String[] args) {
        System.out.println("password: "+new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret"));
        SpringApplication.run(MyappApplication.class, args);
	}

}
