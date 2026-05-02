package com.tex.cloud_task_manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tex")
public class CloudTaskManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudTaskManagerApplication.class, args);
	}

}
